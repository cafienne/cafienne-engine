package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.actormodel.response.{CommandFailure, EngineChokedFailure, ModelResponse, SecurityFailure}
import org.cafienne.cmmn.actorapi.response.{CaseNotModifiedResponse, CaseResponse}
import org.cafienne.humantask.actorapi.response.HumanTaskResponse
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.platform.actorapi.response.{PlatformResponse, PlatformUpdateStatus}
import org.cafienne.service.Main
import org.cafienne.service.api.Headers
import org.cafienne.tenant.actorapi.response.{TenantOwnersResponse, TenantResponse}

import scala.util.{Failure, Success}

trait CommandRoute extends AuthenticatedRoute {

  import akka.pattern.ask

  implicit val timeout: Timeout = Main.caseSystemTimeout

  def askModelActor(command: ModelCommand[_]): Route = {
    onComplete(caseSystem.router() ? command) {
      case Success(value) =>
        value match {
          case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
          case _: EngineChokedFailure => complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
          case e: CommandFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case value: HumanTaskResponse => completeWithLMH(StatusCodes.Accepted, value)
          case value: CaseNotModifiedResponse => complete(StatusCodes.NotModified, "Transition has no effect")
          case value: CaseResponse => completeWithLMH(StatusCodes.OK, value)
          case value: TenantOwnersResponse => complete(StatusCodes.OK, value)
          case value: TenantResponse => completeOnlyLMH(StatusCodes.NoContent, value, Headers.TENANT_LAST_MODIFIED)
          case value: PlatformUpdateStatus => complete(StatusCodes.OK, value) // We should avoid returning a last modified header, as there is a fully asynchronous operation as of now.
          case _: PlatformResponse =>complete(StatusCodes.Accepted, "Handling is in progress") // We should avoid returning a last modified header, as there is a fully asynchronous operation as of now.
          case other => // Unknown new type of response that is not handled
            logger.error(s"Received an unexpected response after asking CaseSystem a command of type ${command.getCommandDescription}. Response is of type ${other.getClass.getSimpleName}")
            complete(StatusCodes.OK)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }

  /**
    * Complete by marshalling the response as JSON and with writing last modified header
    */
  private def completeWithLMH[R <: ModelResponse](statusCode: StatusCodes.Success, response: R, header: String = Headers.CASE_LAST_MODIFIED) = {
    writeLastModifiedHeader(response, header) {
      complete(statusCode, response)
    }
  }

  /**
    * Complete without a response but still with writing last modified header
    */
  private def completeOnlyLMH[R <: ModelResponse](statusCode: StatusCodes.Success, response: R, header: String) = {
    writeLastModifiedHeader(response, header) {
      complete(statusCode)
    }
  }
}
