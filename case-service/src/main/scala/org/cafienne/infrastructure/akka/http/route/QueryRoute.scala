package org.cafienne.infrastructure.akka.http.route

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives.{complete, onComplete, optionalHeaderValueByName}
import akka.http.scaladsl.server.{Directive1, Route}
import org.cafienne.actormodel.exception.AuthorizationException
import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.json.{CafienneJson, Value}
import org.cafienne.service.api.Headers
import org.cafienne.service.api.cases.CaseDefinitionDocument
import org.cafienne.service.db.materializer.LastModifiedRegistration
import org.cafienne.service.db.query.exception.SearchFailure

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait QueryRoute extends AuthenticatedRoute {

  implicit val ec: ExecutionContext = caseSystem.system.dispatcher
  implicit val lastModifiedRegistration: LastModifiedRegistration

  val lastModifiedHeaderName: String = Headers.CASE_LAST_MODIFIED

  def readLastModifiedHeader(): Directive1[Option[String]] = {
    optionalHeaderValueByName(lastModifiedHeaderName)
  }

  def runXMLQuery(future: => Future[CaseDefinitionDocument]): Route = {
    readLastModifiedHeader() { caseLastModified =>
      handleXMLResult(handleSyncedQuery(() => future, caseLastModified))
    }
  }

  def handleXMLResult(future: => Future[CaseDefinitionDocument]): Route = {
    onComplete(future) {
      case Success(value) => complete(StatusCodes.OK, HttpEntity(ContentTypes.`text/xml(UTF-8)`, value.xml))
      case Failure(t) => handleFailure(t)
    }
  }

  def runQuery[T <: CafienneJson](future: => Future[T]): Route = {
    readLastModifiedHeader() { caseLastModified =>
      handleQueryResult(handleSyncedQuery(() => future, caseLastModified))
    }
  }

  def runListQuery[T <: CafienneJson](future: => Future[Seq[T]]): Route = {
    readLastModifiedHeader() { caseLastModified =>
      handleQueryResultList(handleSyncedQuery(() => future, caseLastModified))
    }
  }

  def handleQueryResult[T <: CafienneJson](future: => Future[T]): Route = {
    onComplete(future) {
      case Success(value) => completeJsonValue(value.toValue)
      case Failure(t) => handleFailure(t)
    }
  }

  def handleQueryResultList[T <: CafienneJson](future: => Future[Seq[T]]): Route = {
    onComplete(future) {
      case Success(value) => completeJsonValue(Value.convert(value.map(v => v.toValue)))
      case Failure(t) => handleFailure(t)
    }
  }

  def handleFailure(t: Throwable): Route = {
    t match {
      case notFound: SearchFailure => complete(StatusCodes.NotFound, notFound.getLocalizedMessage)
      case notAllowed: AuthorizationException => complete(StatusCodes.Unauthorized, notAllowed.getMessage)
      case notAllowed: SecurityException => complete(StatusCodes.Unauthorized, notAllowed.getMessage)
      case error =>
        logger.warn("Bumped into an exception " + t.getMessage)
        logger.whenDebugEnabled(logger.debug("Failure while handling query", error))
        complete(StatusCodes.InternalServerError)
    }
  }

  def handleSyncedQuery[A](query: () => Future[A], clm: Option[String]): Future[A] = {
    clm match {
      case Some(s) =>
        // Now go to the writer and ask it to wait for the clm for this case instance id...
        val promise = lastModifiedRegistration.waitFor(new ActorLastModified(s))
        promise.future.flatMap(_ => query())
      case None => // Nothing to do, just continue
        query()
    }
  }
}
