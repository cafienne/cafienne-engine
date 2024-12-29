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

package org.cafienne.service.http.tasks

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.{CaseUserIdentity, UserIdentity}
import org.cafienne.humantask.actorapi.command.HumanTaskCommand
import org.cafienne.infrastructure.http.route.CaseTeamValidator
import org.cafienne.querydb.query._
import org.cafienne.querydb.query.exception.TaskSearchFailure
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.service.http.cases.CasesRoute

import scala.util.{Failure, Success}

trait TaskRoute extends CasesRoute with CaseTeamValidator {
  val taskQueries: TaskQueries = new TaskQueriesImpl(QueryDBSchema._db)

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
    def apply(caseInstanceId: String, user: CaseMembership, member: CaseUserIdentity): HumanTaskCommand
  }

  trait CreateTaskCommand {
    def apply(caseInstanceId: String, user: CaseMembership): HumanTaskCommand
  }
}
