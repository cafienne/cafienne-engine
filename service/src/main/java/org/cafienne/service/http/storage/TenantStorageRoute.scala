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
import jakarta.ws.rs.{DELETE, Path, Produces}
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.ActorType
import org.cafienne.service.http.CaseEngineHttpServer
import org.cafienne.service.http.tenant.route.TenantRoute
import org.cafienne.storage.StorageUser
import org.cafienne.storage.actormodel.ActorMetadata

import scala.util.{Failure, Success}

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
@Path("/storage/tenant/{tenant}")
class TenantStorageRoute(override val httpService: CaseEngineHttpServer) extends TenantRoute with StorageRoute {
  override val prefix = "tenant"
  override def routes: Route = concat(deleteTenant)

  @Path("")
  @DELETE
  @Operation(
    summary = "Remove the tenant from the system",
    description = "Delete all tenant data from the event journal and the query database",
    tags = Array("storage"),
    parameters = Array(
      new Parameter(
        name = "tenant",
        description = "Unique id of the tenant",
        in = ParameterIn.PATH,
        schema = new Schema(implementation = classOf[String]),
        required = true
      )
    ),
    responses = Array(
      new ApiResponse(description = "Tenant removal initiated", responseCode = "202"),
      new ApiResponse(description = "Tenant not found", responseCode = "404")
    )
  )
  @Produces(Array("application/json"))
  def deleteTenant: Route = delete {
      tenantUser { user =>
        if (!user.isOwner) {
          complete(StatusCodes.Unauthorized, "Only tenant owners can perform this operation")
        } else {
          onComplete(tenantQueries.getTenantGroupsUsage(user, user.tenant)) {
            case Success(usageResult) =>
              usageResult.size match {
                case 0 => initiateDataRemoval(ActorMetadata(user = StorageUser(user.id, user.tenant), actorType = ActorType.Tenant, actorId = user.tenant))
                case _ =>
//                  val json = Value.convert(usageResult)
//                  println("Found groups in use somewhere else: " + json)

                  val cases = usageResult.values.flatMap(_.values).flatten.toSet
                  val numCasesUsingGroups = cases.size
//                  println("NUmCases: " + numCasesUsingGroups)
                  val numTenantsUsingGroups = usageResult.flatMap(_._2.keys).toSet.size
//                  println("NumTenants: " + numTenantsUsingGroups)
                  val groups = usageResult.keys.mkString("\n- ", "\n- ", "")
                  val errorPostfix = {
                    (numCasesUsingGroups, numTenantsUsingGroups) match {
                      case (1, 1) => s"a case in another tenant:$groups"
                      case (_, 1) => s"$numCasesUsingGroups cases in another tenant:$groups"
                      case _ => s"$numCasesUsingGroups cases across $numTenantsUsingGroups tenants:$groups"
                    }
                  }
                  usageResult.size match {
                    case 1 => complete(StatusCodes.BadRequest, s"Tenant has a group that is used in $errorPostfix")
                    case moreThanOne => complete(StatusCodes.BadRequest, s"Tenant has $moreThanOne groups that are used in $errorPostfix")
                  }

                  // ALTERNATE Implementation that gives tenant and case details back to the user
//                  val json = Value.convert(usageResult)
//                  println("Found groups in use somewhere else: " + json)
//                  import org.apache.pekko.http.scaladsl.marshalling.Marshaller
//                  import org.apache.pekko.http.scaladsl.model.HttpEntity
//                  import org.apache.pekko.http.scaladsl.model.ContentTypes
//                  implicit val valueMarshaller: Marshaller[Value[_], HttpEntity.Strict] = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: Value[_] =>
//                    HttpEntity(ContentTypes.`application/json`, value.toString)
//                  }
//                  complete(StatusCodes.BadRequest, json)
              }
            case Failure(exception) => throw exception // will be caught by default exception handler
          }
        }
      }
  }
}
