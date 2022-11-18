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

package org.cafienne.infrastructure.akkahttp.route

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.{Directive1, Route}
import org.cafienne.actormodel.exception.AuthorizationException
import org.cafienne.actormodel.response.ActorLastModified
import org.cafienne.json.{CafienneJson, Value}
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.query.exception.SearchFailure
import org.cafienne.service.akkahttp.Headers
import org.cafienne.service.akkahttp.cases.CaseDefinitionDocument

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait QueryRoute extends AuthenticatedRoute {

  implicit val ec: ExecutionContext = caseSystem.system.dispatcher
  val lastModifiedRegistration: LastModifiedRegistration

  val lastModifiedHeaderName: String = Headers.CASE_LAST_MODIFIED

  def readLastModifiedHeader(): Directive1[Option[String]] = {
    optionalHeaderValueByName(lastModifiedHeaderName)
  }

  def runXMLQuery(future: => Future[CaseDefinitionDocument]): Route = {
    readLastModifiedHeader() { caseLastModified =>
      handleXMLResult(runSyncedQuery(future, caseLastModified))
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
      handleQueryResult(runSyncedQuery(future, caseLastModified))
    }
  }

  def runListQuery[T <: CafienneJson](future: => Future[Seq[T]]): Route = {
    readLastModifiedHeader() { caseLastModified =>
      handleQueryResultList(runSyncedQuery(future, caseLastModified))
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

  /**
    * Runs the query after last modified registration has handled the optional last modified timestamp
    * If the last modified timestamp is empty, then the query is ran immediately.
    * @param query
    * @param lastModified
    * @tparam A
    * @return
    */
  def runSyncedQuery[A](query: => Future[A], lastModified: Option[String] = None): Future[A] = {
    lastModified match {
      case Some(s) =>
        // Now go to the writer and ask it to wait for the clm for this case instance id...
        val promise = lastModifiedRegistration.waitFor(new ActorLastModified(s))
        promise.future.flatMap(_ => query)
      case None => // Nothing to do, just continue
        query
    }
  }
}
