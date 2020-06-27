package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete, optionalHeaderValueByName}
import akka.http.scaladsl.server.Route
import org.cafienne.cmmn.akka.command.response.CaseLastModified
import org.cafienne.cmmn.instance.casefile.Value
import org.cafienne.infrastructure.json.CafienneJson
import org.cafienne.service.api
import org.cafienne.service.api.projection.{LastModifiedRegistration, SearchFailure}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait QueryRoute extends AuthenticatedRoute {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val lastModifiedRegistration: LastModifiedRegistration

  def runQuery(future: => Future[CafienneJson]): Route = {
    optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
      handleQueryResult(handleSyncedQuery(() => future, caseLastModified))
    }
  }

  def runListQuery(future: => Future[Seq[CafienneJson]]): Route = {
    optionalHeaderValueByName(api.CASE_LAST_MODIFIED) { caseLastModified =>
      handleQueryResultList(handleSyncedQuery(() => future, caseLastModified))
    }
  }

  def handleQueryResult(future: => Future[CafienneJson]): Route = {
    onComplete(future) {
      case Success(value) => completeJsonValue(value.toValue)
      case Failure(t) => handleFailure(t)
    }
  }

  def handleQueryResultList(future: => Future[Seq[CafienneJson]]): Route = {
    onComplete(future) {
      case Success(value) => completeJsonValue(Value.convert(value.seq.map(v => v.toValue)))
      case Failure(t) => handleFailure(t)
    }
  }

  def handleFailure(t: Throwable): Route = {
    t match {
      case notFound: SearchFailure => complete(StatusCodes.NotFound, notFound.getLocalizedMessage)
      case notAllowed: SecurityException => complete(StatusCodes.Unauthorized, notAllowed.getMessage)
      case error => {
        logger.whenDebugEnabled(() => logger.debug("Failure while handling query", error))
        complete(StatusCodes.InternalServerError)
      }
    }
  }

  def handleSyncedQuery[A](query: () => Future[A], clm: Option[String]): Future[A] = {
    clm match {
      case Some(s) =>
        // Now go to the writer and ask it to wait for the clm for this case instance id...
        val promise = lastModifiedRegistration.waitFor(new CaseLastModified(s))
        promise.future.flatMap(_ => query())
      case None => // Nothing to do, just continue
        query()
    }
  }
}
