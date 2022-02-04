package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, onComplete, respondWithHeader, respondWithHeaders}
import akka.http.scaladsl.server.{Directive0, Route}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.actormodel.response.{CommandFailure, EngineChokedFailure, ModelResponse, SecurityFailure}
import org.cafienne.cmmn.actorapi.response.{CaseNotModifiedResponse, CaseResponse}
import org.cafienne.consentgroup.actorapi.response.{ConsentGroupCreatedResponse, ConsentGroupResponse}
import org.cafienne.humantask.actorapi.response.HumanTaskResponse
import org.cafienne.infrastructure.akkahttp.ResponseMarshallers._
import org.cafienne.service.api.Headers
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.response.{TenantOwnersResponse, TenantResponse}

import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

trait CommandRoute extends AuthenticatedRoute {
  def askModelActor(command: ModelCommand): Route = {
    CommandRouteExecutor.askModelActor(caseSystem, command)
  }
}

object CommandRouteExecutor extends LazyLogging {
  def askModelActor(caseSystem: CaseSystem, command: ModelCommand): Route = {
    onComplete(caseSystem.gateway.request(command)) {
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
          case value: ConsentGroupCreatedResponse => completeWithLMH(StatusCodes.OK, value, Headers.CONSENT_GROUP_LAST_MODIFIED)
          case value: ConsentGroupResponse => completeOnlyLMH(StatusCodes.Accepted, value, Headers.CONSENT_GROUP_LAST_MODIFIED)
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
  private def completeWithLMH[R <: ModelResponse](statusCode: StatusCodes.Success, response: R, header: String = Headers.CASE_LAST_MODIFIED): Route = {
    writeLastModifiedHeader(response, header) {
      complete(statusCode, response)
    }
  }

  /**
    * Complete without a response but still with writing last modified header
    */
  private def completeOnlyLMH[R <: ModelResponse](statusCode: StatusCodes.Success, response: R, header: String): Route = {
    writeLastModifiedHeader(response, header) {
      complete(statusCode)
    }
  }

  def writeLastModifiedHeader(response: ModelResponse, headerName: String = Headers.CASE_LAST_MODIFIED): Directive0 = {
    val lm = response.lastModifiedContent().toString
    if (lm != null) {
      respondWithHeader(RawHeader(headerName, response.lastModifiedContent.toString))
    } else {
      respondWithHeaders(Seq())
    }
  }
}
