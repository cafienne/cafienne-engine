package org.cafienne.cmmn.instance.casefile.document

import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.server.Route
import org.cafienne.akka.actor.identity.TenantUser
import org.cafienne.cmmn.instance.casefile.Path

import scala.concurrent.Future

trait DocumentService {

  def upload(user: TenantUser, caseInstanceId: String, path: Path, bodyPart: FormData.BodyPart): Future[DocumentIdentifier]

  def download(user: TenantUser, caseInstanceId: String, path: Path): Route

  def removeUploads(user: TenantUser, caseInstanceId: String, path: Path, identifiers: Seq[DocumentIdentifier]): Unit
}
