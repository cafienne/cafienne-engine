/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tasks

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.humantask.akka.command.HumanTaskCommand
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.api.projection.TaskSearchFailure
import org.cafienne.service.api.projection.query.TaskQueries

import scala.util.{Failure, Success}

trait TaskRoute extends CommandRoute with QueryRoute {
  val taskQueries: TaskQueries

  override val lastModifiedRegistration = CaseReader.lastModifiedRegistration

  def askTask(platformUser: PlatformUser, taskId: String, createTaskCommand: CreateTaskCommand): Route = {
    onComplete(taskQueries.authorizeTaskAccessAndReturnCaseAndTenantId(taskId, platformUser)) {
      case Success((caseInstanceId, tenant)) => askModelActor(createTaskCommand.apply(caseInstanceId, platformUser.getTenantUser(tenant)))
      case Failure(error) => {
        error match {
          case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
          case _ => throw error
        }
      }
    }
  }

  trait CreateTaskCommand {
    def apply(caseInstanceId: String, user: TenantUser): HumanTaskCommand
  }
}
