/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.response.CommandFailure
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.akka.command._
import org.cafienne.cmmn.akka.command.response.CaseResponse
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.service.api.AuthenticatedRoute
import org.cafienne.service.api.cases.CaseQueries
import org.cafienne.service.{Main, api}

import scala.util.{Failure, Success}

trait CasesRoute extends AuthenticatedRoute {
  val caseQueries: CaseQueries

  def askCase(platformUser: PlatformUser, caseInstanceId: String, createTaskCommand: CreateCaseCommand) = {
    onComplete(caseQueries.getTenantInformation(caseInstanceId, platformUser)) {
      case Success(retrieval) => {
        retrieval match {
          case Some(tenant) => invokeCase(createTaskCommand.apply(platformUser.getTenantUser(tenant)))
          case None => complete(StatusCodes.NotFound, "A case with id " + caseInstanceId + " cannot be found in the system")
        }
      }
      case Failure(error) => complete(StatusCodes.InternalServerError, error)
    }
  }

  implicit val timeout = Main.caseSystemTimeout

  def invokeCase(command: CaseCommand) = {
    onComplete(CaseSystem.router ? command) {
      case Success(value) =>
        value match {
          case e: CommandFailure =>
            complete(StatusCodes.BadRequest, e.exception.getMessage)
          case value: CaseResponse =>
            respondWithHeader(RawHeader(api.CASE_LAST_MODIFIED, value.lastModifiedContent().toString)) {
              complete(StatusCodes.OK, value)
            }
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }

  trait CreateCaseCommand {
    def apply(user: TenantUser): CaseCommand
  }

}
