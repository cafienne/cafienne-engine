package org.cafienne.cmmn.instance.casefile.document

import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.server.Directives.getFromFile
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.FileIO
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.config.util.ConfigurationException
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.instance.casefile.Path

import java.io.File
import scala.concurrent.Future

class FileBasedDocumentStorage extends DocumentService with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val materializer = CaseSystem.system

  val storageDirectory: File = {
    val location = CaseSystem.config.engine.documentService.location
    val file = new File(location)
    if (!file.exists) {
      throw ConfigurationException(s"Storage location '$location' does not exist")
    }
    if (!file.isDirectory) {
      throw ConfigurationException(s"Storage location '$location' must be a directory")
    }
    logger.info(s"Storage location for uploading & downloading documents ${file.getAbsolutePath}")
    file
  }

  val casesDirectory: File = {
    val location = storageDirectory + File.separator + "cases"
    val file = new File(location)
    if (!file.exists) {
      file.mkdirs()
    }

    if (! file.isDirectory) {
      throw ConfigurationException(s"Storage location for cases '$location' must be a directory")
    }

    logger.info(s"Storage location for uploading & downloading documents ${file.getAbsolutePath}")
    file
  }

  override def upload(user: TenantUser, caseInstanceId: String, path: Path, bodyPart: FormData.BodyPart): Future[DocumentIdentifier] = {

    // stream into a file as the chunks of it arrives and return a future
    // file to where it got stored
    val newFileName = bodyPart.filename.getOrElse({
      println("GENERATING NEW NAME.TXT")
      "new_name.txt"
    })

    val file: File = new File(getPathDirectory(caseInstanceId, path, true), newFileName)
    bodyPart.entity.dataBytes.runWith(FileIO.toPath(file.toPath)).map(_ => bodyPart.name -> file).map(result => {
      logger.info("Uploaded " + file.getAbsolutePath)
      DocumentIdentifier(newFileName)
    })
  }

  private def getPathDirectory(caseInstanceId: String, path: Path, createIfNotExists: Boolean = false): File = {
    val caseInstanceDirectory: File = getCaseDirectory(caseInstanceId, createIfNotExists)
    val caseFileItemPath = new File(caseInstanceDirectory, path.toString)
    if (createIfNotExists && !caseFileItemPath.exists()) {
      caseFileItemPath.mkdirs()
    }
    caseFileItemPath
  }

  private def getCaseDirectory(caseInstanceId: String, createIfNotExists: Boolean = false): File = {
    val caseInstancePath: File = new File(casesDirectory, caseInstanceId)
    if (createIfNotExists && !caseInstancePath.exists()) {
      caseInstancePath.mkdir()
    }
    caseInstancePath
  }

  override def download(user: TenantUser, caseInstanceId: String, path: Path): Route = {
    val directory = getPathDirectory(caseInstanceId, path)
    getFromFile(directory)
  }

  override def removeUploads(user: TenantUser, caseInstanceId: String, path: Path, identifiers: Seq[DocumentIdentifier]): Unit = {
    val directory: File = getPathDirectory(caseInstanceId, path)
    logger.info(s"Removing upload $identifiers from directory $directory")
    if (directory.exists()) {
      identifiers.map(i => i.identifier).foreach(filename => {
        logger.info(s"  Deleting $filename from directory ${directory.getAbsolutePath}")
        new File(directory, filename).delete()
      })
      directoryCleaner(directory, getCaseDirectory(caseInstanceId))
    } else {
      logger.warn(s"Trying to remove uploads $identifiers, but directory $directory does not exist")
    }
  }

  private def directoryCleaner(directory: File, caseDirectory: File): Unit = {
    if (directory.list().isEmpty) {
      logger.debug("Cleaning empty directory " + directory)
      directory.delete()
      if (directory == caseDirectory) {
        // Done
        logger.debug("Also cleaned case directory")
      } else {
        directoryCleaner(directory.getParentFile, caseDirectory)
      }
    } else {
      logger.debug(s"Directory $directory has ${directory.list.size} files left; will not be cleared further")
    }
  }
}
