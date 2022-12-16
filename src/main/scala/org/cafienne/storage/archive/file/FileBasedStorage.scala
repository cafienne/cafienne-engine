/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.storage.archive.file

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.FileStorageConfig
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.archival.Archive
import org.cafienne.storage.archive.Storage
import org.cafienne.json.{JSONReader, ValueMap}

import java.io.{File, FileInputStream, FileWriter}
import scala.concurrent.Future

class FileBasedStorage(val config: FileStorageConfig) extends Storage with LazyLogging {
  val directory: File = config.directory

  override def store(archive: Archive): Future[Done] = {
    val file = getFile(archive.metadata)
    val writer = new FileWriter(file, true)
    writer.write(archive.toString())
    writer.close()
    logger.whenDebugEnabled(logger.debug(s"Wrote archive to disk: ${file.getName}"))
    Future.successful(Done)
  }

  def getFile(metadata: ActorMetadata): File = {
    val fileName = s"${File.separator}archive-${metadata.actorType.toLowerCase()}-${metadata.actorId}.json"
    new File(directory.getAbsolutePath + fileName)
  }

  override def retrieve(metadata: ActorMetadata): Future[Archive] = {
    val file = getFile(metadata)
    val json = JSONReader.parse(new FileInputStream(file)).asInstanceOf[ValueMap]
    Future.successful(Archive.deserialize(json))
  }
}
