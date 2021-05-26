/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.anonymous

import _root_.akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.exception.SerializedException
import org.cafienne.akka.actor.command.response.{CommandFailure, EngineChokedFailure}
import org.cafienne.cmmn.akka.command.StartCase
import org.cafienne.infrastructure.akka.http.route.CaseServiceRoute
import org.cafienne.service.Main

import javax.ws.rs._
import scala.util.{Failure, Success}

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/request")
class AnonymousRoute extends CaseServiceRoute {

  val defaultErrorMessage = "Your request bumped into an internal configuration issue and cannot be handled"

  def sendCommand[T](command: StartCase, expectedResponseClass: Class[T], expectedResponseHandler: T => Route): Route = {
    import akka.pattern.ask
    implicit val timeout = Main.caseSystemTimeout
    onComplete(CaseSystem.router() ? command) {
      case Success(value) =>
        value.getClass.isAssignableFrom(expectedResponseClass) match {
          case true => expectedResponseHandler(value.asInstanceOf[T])
          case false => failOnCaseSystemResponse(value)
        }
      case Failure(e) => fail(e)
    }
  }

  def failOnCaseSystemResponse(response: Any): Route = {
    response match {
      case e: EngineChokedFailure => fail(e)
      case e: CommandFailure => fail(e.exception())
      case other => fail("Unexpected response of type " + other.getClass.getName)
    }
  }

  def fail(e: EngineChokedFailure) = {
    extractUri { uri =>
      logger.warn(s"Route $uri bumped into a choked engine with failure of type ${e.exception().getClassName}: " + e.exception().getMessage)
      complete(StatusCodes.InternalServerError, "An error happened in the server; check the server logs for more information")
    }
  }

  def fail(t: SerializedException) = {
    completeFailure(t.getMessage, " of type " + t.getClassName)
  }

  def fail(msg: String) = {
    completeFailure(msg)
  }

  def fail(t: Throwable) = {
    completeFailure(t.getMessage, " of type " + t.getClass.getName)
  }

  private def completeFailure(msg: String, msgType: String = ""): Route = {
    extractUri { uri =>
      logger.warn(s"Anonymous route $uri encountered a configuration failure$msgType: " + msg)
      complete(StatusCodes.InternalServerError, defaultErrorMessage)
    }
  }
}