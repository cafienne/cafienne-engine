/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.anonymous

import _root_.akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import org.cafienne.actormodel.exception.SerializedException
import org.cafienne.actormodel.response.{CommandFailure, EngineChokedFailure}
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.infrastructure.akkahttp.route.CaseServiceRoute

import scala.util.{Failure, Success}

trait AnonymousRoute extends CaseServiceRoute {

  val defaultErrorMessage = "Your request bumped into an internal configuration issue and cannot be handled"

  def sendCommand[T](command: StartCase, expectedResponseClass: Class[T], expectedResponseHandler: T => Route): Route = {
    onComplete(caseSystem.gateway.request(command)) {
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