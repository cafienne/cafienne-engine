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

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.exception.AuthorizationException
import org.cafienne.json.{CafienneJson, Value}
import org.cafienne.querydb.query.exception.SearchFailure
import org.cafienne.service.akkahttp.LastModifiedHeader

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait QueryRoute extends AuthenticatedRoute {

  implicit val ec: ExecutionContext = caseSystem.system.dispatcher

  val lastModifiedHeaderName: String

  def readLastModifiedHeader()(subRoute: LastModifiedHeader => Route): Route = {
    readLastModifiedHeader(lastModifiedHeaderName)(subRoute)
  }

  def runQuery[T <: CafienneJson](future: => Future[T]): Route = {
    readLastModifiedHeader() { lastModified => handleQueryResult(runSyncedQuery(future, lastModified)) }
  }

  def runListQuery[T <: CafienneJson](future: => Future[Seq[T]]): Route = {
    readLastModifiedHeader() { lastModified => handleQueryResultList(runSyncedQuery(future, lastModified)) }
  }

  def handleQueryResult[T <: CafienneJson](future: => Future[T]): Route = {
    onComplete(future) {
      case Success(value) => completeJson(value.toValue)
      case Failure(t) => handleFailure(t)
    }
  }

  def handleQueryResultList[T <: CafienneJson](future: => Future[Seq[T]]): Route = {
    onComplete(future) {
      case Success(value) => completeJson(Value.convert(value.map(v => v.toValue)))
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
    *
    * @param query
    * @param lastModified
    * @tparam A
    * @return
    */
  def runSyncedQuery[A](query: => Future[A], lastModified: LastModifiedHeader): Future[A] = {
    lastModified.available.flatMap(_ => query)
  }
}
