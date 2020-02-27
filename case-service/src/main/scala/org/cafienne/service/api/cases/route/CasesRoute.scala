/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.akka.command._
import org.cafienne.infrastructure.akka.http.route.CommandRoute
import org.cafienne.service.api.cases.CaseQueries

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute {
  val caseQueries: CaseQueries

  def askCase(platformUser: PlatformUser, caseInstanceId: String, createCaseCommand: CreateCaseCommand) = {
    onComplete(caseQueries.getTenantInformation(caseInstanceId, platformUser)) {
      case Success(retrieval) => {
        retrieval match {
          case Some(tenant) => askModelActor(createCaseCommand.apply(platformUser.getTenantUser(tenant)))
          case None => complete(StatusCodes.NotFound, "A case with id " + caseInstanceId + " cannot be found in the system")
        }
      }
      case Failure(error) => complete(StatusCodes.InternalServerError, error)
    }
  }

  trait CreateCaseCommand {
    def apply(user: TenantUser): CaseCommand
  }

}
