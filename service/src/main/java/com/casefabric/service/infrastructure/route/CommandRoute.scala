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

package com.casefabric.service.infrastructure.route

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives.{complete, onComplete}
import org.apache.pekko.http.scaladsl.server.Route
import com.casefabric.actormodel.command.ModelCommand
import com.casefabric.actormodel.response._
import com.casefabric.cmmn.actorapi.response.{CaseNotModifiedResponse, CaseResponse}
import com.casefabric.consentgroup.actorapi.response.{ConsentGroupCreatedResponse, ConsentGroupResponse}
import com.casefabric.humantask.actorapi.response.HumanTaskResponse
import com.casefabric.querydb.lastmodified.Headers
import com.casefabric.querydb.query.exception._
import com.casefabric.storage.actormodel.ActorType
import com.casefabric.system.CaseSystem
import com.casefabric.tenant.actorapi.response.{TenantOwnersResponse, TenantResponse}

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
          case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
          case i: ActorInStorage => complete(StatusCodes.NotFound, readSearchFailure(i.actorType, i.getActorId).getLocalizedMessage)
          case _: EngineChokedFailure => complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
          case e: ActorExistsFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case e: CommandFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case value: HumanTaskResponse => completeWithLMH(StatusCodes.Accepted, value, Headers.CASE_LAST_MODIFIED)
          case _: CaseNotModifiedResponse => complete(StatusCodes.NotModified, "Transition has no effect")
          case value: CaseResponse => completeWithLMH(StatusCodes.OK, value, Headers.CASE_LAST_MODIFIED)
          case value: TenantOwnersResponse => complete(StatusCodes.OK, value)
          case value: TenantResponse => completeOnlyLMH(StatusCodes.NoContent, value, Headers.TENANT_LAST_MODIFIED)
          case value: ConsentGroupCreatedResponse => completeWithLMH(StatusCodes.OK, value, Headers.CONSENT_GROUP_LAST_MODIFIED)
          case value: ConsentGroupResponse => completeOnlyLMH(StatusCodes.Accepted, value, Headers.CONSENT_GROUP_LAST_MODIFIED)
          case other => // Unknown new type of response that is not handled
            logger.error(s"Received an unexpected response after asking CaseSystem a command of type ${command.getCommandDescription}. Response is of type ${other.getClass.getSimpleName}")
            complete(StatusCodes.OK)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }

  def readSearchFailure(actorType: String, actorId: String): SearchFailure = {
    actorType match {
      case ActorType.Case => CaseSearchFailure(actorId)
      case ActorType.Tenant => TenantSearchFailure(actorId)
      case ActorType.Process => TaskSearchFailure(actorId)
      case ActorType.Group => ConsentGroupSearchFailure(actorId)
      case _ =>
        logger.warn(s"Received 'ActorInStorage' response with unknown actor type $actorType on actor $actorId")
        CaseSearchFailure(actorId)
    }
  }
}
