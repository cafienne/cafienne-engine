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

package org.cafienne.service.http.anonymous

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{ExceptionHandler, Route}
import org.cafienne.actormodel.exception.SerializedException
import org.cafienne.actormodel.response.{CommandFailure, EngineChokedFailure}
import org.cafienne.engine.actorapi.CaseFamily
import org.cafienne.engine.cmmn.actorapi.command.StartCase
import org.cafienne.service.infrastructure.route.CaseServiceRoute

import scala.util.{Failure, Success}

trait AnonymousRoute extends CaseServiceRoute {

  val defaultErrorMessage = "Your request bumped into an internal configuration issue and cannot be handled"

  def sendCommand[T](command: StartCase, expectedResponseClass: Class[T], expectedResponseHandler: T => Route): Route = {
    onComplete(caseSystem.engine.request(CaseFamily(command.actorId), command)) {
      case Success(value) =>
        if (value.getClass.isAssignableFrom(expectedResponseClass)) {
          expectedResponseHandler(value.asInstanceOf[T])
        } else {
          failOnCaseSystemResponse(value)
        }
      case Failure(e) => fail(e)
    }
  }

  override def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case exception: Throwable => fail(exception)
  }

  def failOnCaseSystemResponse(response: Any): Route = {
    response match {
      case e: EngineChokedFailure => fail(e)
      case e: CommandFailure => fail(e.exception())
      case other => fail("Unexpected response of type " + other.getClass.getName)
    }
  }

  def fail(e: EngineChokedFailure): Route = {
    extractUri { uri =>
      logger.warn(s"Route $uri bumped into a choked engine with failure of type ${e.exception().getClassName}: " + e.exception().getMessage)
      complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
    }
  }

  def fail(t: SerializedException): Route = {
    completeFailure(t.getMessage, " of type " + t.getClassName)
  }

  def fail(msg: String): Route = {
    completeFailure(msg)
  }

  def fail(t: Throwable): Route = {
    completeFailure(t.getMessage, " of type " + t.getClass.getName)
  }

  private def completeFailure(msg: String, msgType: String = ""): Route = {
    extractUri { uri =>
      logger.warn(s"Anonymous route $uri encountered a configuration failure$msgType: " + msg)
      complete(StatusCodes.InternalServerError, defaultErrorMessage)
    }
  }
}