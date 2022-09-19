/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.tasks

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{CaseUserIdentity, UserIdentity}
import org.cafienne.humantask.actorapi.command.WorkflowCommand
import org.cafienne.infrastructure.akkahttp.route.CaseTeamValidator
import org.cafienne.querydb.query._
import org.cafienne.querydb.query.exception.TaskSearchFailure
import org.cafienne.service.akkahttp.cases.route.CasesRoute

import scala.util.{Failure, Success}

trait TaskRoute extends CasesRoute with CaseTeamValidator {
  val taskQueries: TaskQueries = new TaskQueriesImpl

  def askTaskWithAssignee(user: UserIdentity, taskId: String, assignee: String, createTaskCommand: CreateTaskCommandWithAssignee): Route = {
    onComplete(taskQueries.getCaseMembership(taskId, user)) {
      case Success(caseMember) =>
        val caseInstanceId = caseMember.caseInstanceId
        onComplete(getUserOrigin(assignee, caseMember.tenant)) {
          case Success(assigneeIdentity) =>
//            println(s"Found origin $origin for assignee $assignee")
            askModelActor(createTaskCommand.apply(caseInstanceId, caseMember, assigneeIdentity))
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

  def askTask(user: UserIdentity, taskId: String, createTaskCommand: CreateTaskCommand): Route = {
    onComplete(taskQueries.getCaseMembership(taskId, user)) {
      case Success(caseMember) => askModelActor(createTaskCommand.apply(caseMember.caseInstanceId, caseMember))
      case Failure(error) => error match {
        case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
        case _ => throw error
      }
    }
  }

  trait CreateTaskCommandWithAssignee {
    def apply(caseInstanceId: String, user: CaseMembership, member: CaseUserIdentity): WorkflowCommand
  }

  trait CreateTaskCommand {
    def apply(caseInstanceId: String, user: CaseMembership): WorkflowCommand
  }
}
