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

package org.cafienne.service.akkahttp.storage

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.{Operation, Parameter}
import org.cafienne.service.akkahttp.cases.route.CasesRoute
import org.cafienne.service.akkahttp.tenant.route.TenantRoute
import org.cafienne.storage.StorageUser
import org.cafienne.storage.actormodel.{ActorMetadata, ActorType}
import org.cafienne.system.CaseSystem

import javax.ws.rs.{DELETE, PUT, Path, Produces}
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/storage")
class CaseStorageRoute(val caseSystem: CaseSystem) extends CasesRoute with TenantRoute with StorageRoute {
  override def routes: Route = concat(archiveCaseInstance, restoreCaseInstance, deleteCaseInstance)

  @Path("/case/{caseInstanceId}/archive")
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
    caseUser { user =>
      path("case" / Segment / "archive") { caseInstanceId =>
        authorizeCaseAccess(user, caseInstanceId, { caseMember => {
          val tenant = caseMember.tenant
          onComplete(getTenantUser(user, tenant, None)) {
            case Success(tenantUser) =>
              if (tenantUser.enabled && tenantUser.isOwner) {
                initiateDataArchival(ActorMetadata(user = StorageUser(user.id), actorType = ActorType.Case, tenant = tenant, actorId = caseInstanceId))
              } else {
                complete(StatusCodes.Unauthorized, "Only tenant owners can perform this operation")
              }
            case Failure(t) => throw t
          }
        }}
        )
      }
    }
  }

  @Path("/case/{caseInstanceId}/restore")
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
    caseUser { user =>
      path("case" / Segment / "archive") { caseInstanceId =>
        authorizeCaseAccess(user, caseInstanceId, { caseMember => {
          val tenant = caseMember.tenant
          onComplete(getTenantUser(user, tenant, None)) {
            case Success(tenantUser) =>
              if (tenantUser.enabled && tenantUser.isOwner) {
                restoreActorData(ActorMetadata(user = StorageUser(user.id), actorType = ActorType.Case, tenant = tenant, actorId = caseInstanceId))
              } else {
                complete(StatusCodes.Unauthorized, "Only tenant owners can perform this operation")
              }
            case Failure(t) => throw t
          }
        }}
        )
      }
    }
  }

  @Path("/case/{caseInstanceId}")
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
    caseUser { user =>
      path("case" / Segment) { caseInstanceId =>
        authorizeCaseAccess(user, caseInstanceId, { caseMember => {
          val tenant = caseMember.tenant
          onComplete(getTenantUser(user, tenant, None)) {
            case Success(tenantUser) =>
              if (tenantUser.enabled && tenantUser.isOwner) {
                initiateDataRemoval(ActorMetadata(user = StorageUser(user.id), actorType = ActorType.Case, tenant = tenant, actorId = caseInstanceId))
              } else {
                complete(StatusCodes.Unauthorized, "Only tenant owners can perform this operation")
              }
            case Failure(t) => throw t
          }
        }}
        )
      }
    }
  }
}
