package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, onComplete, respondWithHeader}
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.ModelCommand
import org.cafienne.akka.actor.command.response.{CommandFailure, SecurityFailure}
import org.cafienne.cmmn.akka.command.response.CaseResponse
import org.cafienne.humantask.akka.command.response.{HumanTaskResponse, HumanTaskValidationResponse}
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.infrastructure.akka.http.ValueMarshallers._
import org.cafienne.service.{Main, api}
import org.cafienne.tenant.akka.command.response.{TenantOwnersResponse, TenantResponse}

import scala.util.{Failure, Success}

trait CommandRoute extends AuthenticatedRoute {


  import akka.pattern.ask
  implicit val timeout = Main.caseSystemTimeout

  def askModelActor(command: ModelCommand[_]) = {
    onComplete(CaseSystem.router ? command) {
      case Success(value) =>
        value match {
          case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
          case e: CommandFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case tenantOwners: TenantOwnersResponse => complete(StatusCodes.OK, tenantOwners)
          case _: TenantResponse => complete(StatusCodes.NoContent)
          case value: HumanTaskValidationResponse =>
            respondWithHeader(RawHeader(api.CASE_LAST_MODIFIED, value.caseLastModified().toString)) {
              complete(StatusCodes.Accepted, value.value())
            }
          case value: HumanTaskResponse =>
            respondWithHeader(RawHeader(api.CASE_LAST_MODIFIED, value.caseLastModified().toString)) {
              complete(StatusCodes.Accepted, value)
            }
          case value: CaseResponse =>
            respondWithHeader(RawHeader(api.CASE_LAST_MODIFIED, value.lastModifiedContent().toString)) {
              complete(StatusCodes.OK, value)
            }
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }

}
