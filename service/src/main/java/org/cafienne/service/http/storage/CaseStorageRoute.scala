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

package org.cafienne.service.http.storage

import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import jakarta.ws.rs.{DELETE, PUT, Path, Produces}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.{Directive, Route}
import org.cafienne.persistence.querydb.query.cmmn.authorization.CaseOwnership
import org.cafienne.persistence.querydb.query.exception.CaseSearchFailure
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.cases.CasesRoute
import org.cafienne.service.http.tenant.route.TenantRoute
import org.cafienne.storage.StorageUser
import org.cafienne.storage.actormodel.{ActorMetadata, ActorType}

import scala.util.{Failure, Success}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/storage/case/{caseInstanceId}")
class CaseStorageRoute(override val httpService: CaseEngineHttpServer) extends CasesRoute with TenantRoute with StorageRoute {
  override val prefix = "case"
  override def routes: Route = concat(archiveCaseInstance, restoreCaseInstance, deleteCaseInstance)

  private def pathMatcher(prefix: String): Directive[Tuple1[String]] = {
    if (prefix.isBlank) pathPrefix(Segment)
    else pathPrefix(Segment / prefix)
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    */
  def caseOwner(subRoute: CaseOwnership => Route): Route = caseOwner("")(subRoute)

  def caseOwner(prefix: String)(subRoute: CaseOwnership => Route, failureMessage: String = "You must be case owner to perform this operation"): Route = {
    authenticatedUser { user =>
      pathMatcher(prefix) { caseInstanceId =>
        readLastModifiedHeader() { caseLastModified =>
          onComplete(runSyncedQuery(authorizationQueries.getCaseOwnership(caseInstanceId, user), caseLastModified)) {
            case Success(caseMember) =>
              if (caseMember.isOwner) subRoute(caseMember)
              else complete(StatusCodes.Unauthorized, failureMessage)
            case Failure(error) =>
              error match {
                case t: CaseSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
                case _ => throw error
              }
          }
        }
      }
    }
  }

  @Path("/archive")
  @PUT
  @Operation(
    summary = "Archive the case instance",
    description = "Archive the case with the specified identifier",
    tags = Array("storage"),
    parameters = Array(
      new Parameter(
        name = "caseInstanceId",
        description = "Unique id of the case instance",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true
      )
    ),
    responses = Array(
      new ApiResponse(description = "Case is archived", responseCode = "201"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def archiveCaseInstance: Route = put {
    caseOwner("archive") { owner =>
      initiateDataArchival(ActorMetadata(user = StorageUser(owner.id, owner.tenant), actorType = ActorType.Case, actorId = owner.caseInstanceId))
    }
  }

  @Path("/restore")
  @PUT
  @Operation(
    summary = "Restore an archived case instance",
    description = "Restore a case instance from the archive",
    tags = Array("storage"),
    parameters = Array(
      new Parameter(
        name = "caseInstanceId",
        description = "Unique id of the case instance",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true
      )
    ),
    responses = Array(
      new ApiResponse(description = "Case is restored", responseCode = "201"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def restoreCaseInstance: Route = put {
    caseInstanceSubRoute("restore") { (user, caseInstanceId) =>
      restoreActorData(ActorMetadata(user = StorageUser(user.id, ""), actorType = ActorType.Case, actorId = caseInstanceId))
    }
  }

  @Path("")
  @DELETE
  @Operation(
    summary = "Remove the case instance from the system",
    description = "Delete all case instance data from the event journal and the query database",
    tags = Array("storage"),
    parameters = Array(
      new Parameter(
        name = "caseInstanceId",
        description = "Unique id of the case instance",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true
      )
    ),
    responses = Array(
      new ApiResponse(description = "Case data is removed", responseCode = "201"),
      new ApiResponse(description = "Case not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def deleteCaseInstance: Route = delete {
    caseOwner { owner =>
      initiateDataRemoval(ActorMetadata(user = StorageUser(owner.id, owner.tenant), actorType = ActorType.Case, actorId = owner.caseInstanceId))
    }
  }
}
