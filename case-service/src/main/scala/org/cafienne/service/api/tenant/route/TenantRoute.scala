/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.Directives._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.response.{CommandFailure, SecurityFailure}
import org.cafienne.infrastructure.akka.http.ResponseMarshallers._
import org.cafienne.service.Main
import org.cafienne.service.api.AuthenticatedRoute
import org.cafienne.tenant.akka.command.TenantCommand
import org.cafienne.tenant.akka.command.response.{TenantOwnersResponse, TenantResponse}

import scala.util.{Failure, Success}

trait TenantRoute extends AuthenticatedRoute {

  override def routes: server.Route = ???

  import akka.pattern.ask
  implicit val timeout = Main.caseSystemTimeout

  def askTenant(command: TenantCommand) = {
    onComplete(CaseSystem.router ? command) {
      case Success(value) =>
        value match {
          case s: SecurityFailure => complete(StatusCodes.Unauthorized, s.exception.getMessage)
          case e: CommandFailure => complete(StatusCodes.BadRequest, e.exception.getMessage)
          case tenantOwners: TenantOwnersResponse => complete(StatusCodes.OK, tenantOwners)
          case _: TenantResponse => complete(StatusCodes.NoContent)
        }
      case Failure(e) => complete(StatusCodes.InternalServerError, e.getMessage)
    }
  }
}
