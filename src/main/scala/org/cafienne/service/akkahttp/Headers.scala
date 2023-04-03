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

package org.cafienne.service.akkahttp

import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.board.state.definition.TeamDefinition
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.materializer.cases.CaseReader
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupReader
import org.cafienne.querydb.materializer.tenant.TenantReader

import scala.concurrent.Future

object Headers {


  final val CASE_LAST_MODIFIED = "Case-Last-Modified"

  final val TENANT_LAST_MODIFIED = "Tenant-Last-Modified"

  final val CONSENT_GROUP_LAST_MODIFIED = "Consent-Group-Last-Modified"

  final val BOARD_LAST_MODIFIED = "Board-Last-Modified"
}

trait LastModifiedHeader {
  val name: String
  val registration: LastModifiedRegistration
  val value: Option[String] = None
  val lastModified: Option[ActorLastModified] = value.map(new ActorLastModified(name, _))

  override def toString: String = name + ": " + value

  def available: Future[String] = {
    if (lastModified.isDefined) {
      //    println("Awaiting " + this)
      registration.waitFor(lastModified.get).future
    } else {
      Future.successful("No header present")
    }
  }
}

case class CaseLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.CASE_LAST_MODIFIED
  override val registration: LastModifiedRegistration = CaseReader.lastModifiedRegistration
}

case class TenantLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.TENANT_LAST_MODIFIED
  override val registration: LastModifiedRegistration = TenantReader.lastModifiedRegistration
}

case class ConsentGroupLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.CONSENT_GROUP_LAST_MODIFIED
  override val registration: LastModifiedRegistration = ConsentGroupReader.lastModifiedRegistration
}

case class BoardLastModifiedHeader(override val value: Option[String]) extends LastModifiedHeader {
  override val name: String = Headers.BOARD_LAST_MODIFIED
  override val registration: LastModifiedRegistration = {
    lastModified.fold(CaseReader.lastModifiedRegistration)(alm => {
      if (alm.actorId.endsWith(TeamDefinition.EXTENSION)) {
        ConsentGroupReader.lastModifiedRegistration
      } else {
        CaseReader.lastModifiedRegistration
      }
    })
  }
}

object LastModifiedHeader {
  val NONE = new LastModifiedHeader {
    override val name: String = ""
    override val registration: LastModifiedRegistration = null
  }
  def get(headerName: String, headerValue: Option[String] = None): LastModifiedHeader = headerName match {
    case Headers.CASE_LAST_MODIFIED => CaseLastModifiedHeader(headerValue)
    case Headers.TENANT_LAST_MODIFIED => TenantLastModifiedHeader(headerValue)
    case Headers.CONSENT_GROUP_LAST_MODIFIED => ConsentGroupLastModifiedHeader(headerValue)
    case Headers.BOARD_LAST_MODIFIED => BoardLastModifiedHeader(headerValue)
    case _ => throw new Exception(s"Unrecognized last modified header $headerName")
  }
}
