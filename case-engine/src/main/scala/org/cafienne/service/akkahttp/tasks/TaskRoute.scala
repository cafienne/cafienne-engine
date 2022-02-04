/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tasks

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{PlatformUser, UserIdentity}
import org.cafienne.humantask.actorapi.command.WorkflowCommand
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.service.db.materializer.LastModifiedRegistration
import org.cafienne.service.db.materializer.cases.CaseReader
import org.cafienne.service.db.query.{CaseMembership, TaskQueries}
import org.cafienne.service.db.query.exception.TaskSearchFailure

import scala.util.{Failure, Success}

trait TaskRoute extends CommandRoute with QueryRoute {
  override val lastModifiedRegistration: LastModifiedRegistration = CaseReader.lastModifiedRegistration
  val taskQueries: TaskQueries

  def askTaskWithAssignee(platformUser: PlatformUser, taskId: String, assignee: String, createTaskCommand: CreateTaskCommandWithAssignee): Route = {
    onComplete(taskQueries.getCaseMembership(taskId, platformUser)) {
      case Success(caseMember) =>
        val caseInstanceId = caseMember.caseInstanceId
        onComplete(userCache.getUserRegistration(assignee)) {
          case Success(assignee: PlatformUser) => askModelActor(createTaskCommand.apply(caseInstanceId, caseMember, assignee))
          case Failure(t: Throwable) =>
            logger.warn(s"An error happened while retrieving user information on user '$assignee'", t)
            complete(StatusCodes.InternalServerError, s"An internal error happened while retrieving user information on user '$assignee'")
        }
      case Failure(error) => error match {
        case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
        case _ => throw error
      }
    }
  }

  def askTask(platformUser: PlatformUser, taskId: String, createTaskCommand: CreateTaskCommand): Route = {
    onComplete(taskQueries.getCaseMembership(taskId, platformUser)) {
      case Success(caseMember) => askModelActor(createTaskCommand.apply(caseMember.caseInstanceId, caseMember))
      case Failure(error) => error match {
        case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
        case _ => throw error
      }

    }
  }

  trait CreateTaskCommandWithAssignee {
    def apply(caseInstanceId: String, user: CaseMembership, member: UserIdentity): WorkflowCommand
  }

  trait CreateTaskCommand {
    def apply(caseInstanceId: String, user: CaseMembership): WorkflowCommand
  }
}
