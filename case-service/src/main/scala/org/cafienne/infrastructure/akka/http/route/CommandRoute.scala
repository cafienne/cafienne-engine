package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import org.cafienne.akka.actor.command.ModelCommand
import org.cafienne.akka.actor.command.response.{CommandFailure, EngineChokedFailure, SecurityFailure}
import org.cafienne.cmmn.actorapi.response.CaseResponse
import org.cafienne.humantask.actorapi.response.{HumanTaskResponse, HumanTaskValidationResponse}
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.platform.actorapi.response.{PlatformResponse, PlatformUpdateStatus}
import org.cafienne.service.Main
import org.cafienne.service.api.Headers
import org.cafienne.tenant.actorapi.response.{TenantOwnersResponse, TenantResponse}

import scala.util.{Failure, Success}

trait CommandRoute extends AuthenticatedRoute {

  import akka.pattern.ask
  implicit val timeout = Main.caseSystemTimeout

  def askModelActor(command: ModelCommand[_]): Route = {
    onComplete(caseSystem.router ? command) {
      case Success(value) =>
        value match {
          case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
          case e: EngineChokedFailure => complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
          case e: CommandFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case tenantOwners: TenantOwnersResponse => complete(StatusCodes.OK, tenantOwners)
          case value: TenantResponse => writeLastModifiedHeader(value, Headers.TENANT_LAST_MODIFIED) {
            complete(StatusCodes.NoContent)
          }
          case value: HumanTaskValidationResponse =>
            writeLastModifiedHeader(value) {
              complete(StatusCodes.Accepted, value.value())
            }
          case value: HumanTaskResponse =>
            writeLastModifiedHeader(value) {
              complete(StatusCodes.Accepted, value)
            }
          case value: CaseResponse =>
            writeLastModifiedHeader(value) {
              complete(StatusCodes.OK, value)
            }
          case value: PlatformUpdateStatus => {
            // We should avoid returning a last modified header, as there is a fully asynchronous operation as of now.
            complete(StatusCodes.OK, value.getValue)
          }
          case _: PlatformResponse => {
            // We should avoid returning a last modified header, as there is a fully asynchronous operation as of now.
            complete(StatusCodes.Accepted, "Handling is in progress")
          }
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }
}
