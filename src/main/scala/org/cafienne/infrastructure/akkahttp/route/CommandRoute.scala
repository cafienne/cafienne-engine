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

package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.actormodel.response._
import org.cafienne.board.actorapi.response.{BoardCreatedResponse, BoardResponse, ColumnAddedResponse}
import org.cafienne.cmmn.actorapi.response.{CaseNotModifiedResponse, CaseResponse}
import org.cafienne.consentgroup.actorapi.response.{ConsentGroupCreatedResponse, ConsentGroupResponse}
import org.cafienne.humantask.actorapi.response.HumanTaskResponse
import org.cafienne.service.akkahttp.Headers
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.response.{TenantOwnersResponse, TenantResponse}

import scala.util.{Failure, Success}

trait CommandRoute extends AuthenticatedRoute {
  def askModelActor(command: ModelCommand): Route = {
    CommandRouteExecutor.askModelActor(caseSystem, command)
  }
}

object CommandRouteExecutor extends LastModifiedDirectives with LazyLogging {
  def askModelActor(caseSystem: CaseSystem, command: ModelCommand): Route = {
    onComplete(caseSystem.gateway.request(command)) {
      case Success(value) =>
        value match {
          case e: CommandFailure => value match {
            case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
            case _: EngineChokedFailure => complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
            case e: ActorExistsFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
            case _ => complete(StatusCodes.BadRequest, e.exception.getMessage)
          }
          case value: CaseResponse => value match {
            case value: HumanTaskResponse => completeWithLMH(StatusCodes.Accepted, value, Headers.CASE_LAST_MODIFIED)
            case _: CaseNotModifiedResponse => complete(StatusCodes.NotModified, "Transition has no effect")
            case other => completeWithLMH(StatusCodes.OK, other, Headers.CASE_LAST_MODIFIED)
          }
          case value: BoardResponse => value match {
            case value: BoardCreatedResponse => completeWithLMH(StatusCodes.Accepted, value, Headers.BOARD_LAST_MODIFIED)
            case value: ColumnAddedResponse => completeWithLMH(StatusCodes.Accepted, value, Headers.BOARD_LAST_MODIFIED)
            case value => completeOnlyLMH(StatusCodes.Accepted, value, Headers.BOARD_LAST_MODIFIED)
          }
          case value: TenantResponse => value match {
            case value: TenantOwnersResponse => complete(StatusCodes.OK, value)
            case _ => completeOnlyLMH(StatusCodes.NoContent, value, Headers.TENANT_LAST_MODIFIED)
          }
          case value: ConsentGroupResponse => value match {
            case value: ConsentGroupCreatedResponse => completeWithLMH(StatusCodes.OK, value, Headers.CONSENT_GROUP_LAST_MODIFIED)
            case _ => completeOnlyLMH(StatusCodes.Accepted, value, Headers.CONSENT_GROUP_LAST_MODIFIED)
          }
          case other => // Unknown new type of response that is not handled
            logger.error(s"Received an unexpected response after asking CaseSystem a command of type ${command.getCommandDescription}. Response is of type ${other.getClass.getSimpleName}")
            complete(StatusCodes.OK)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }
}
