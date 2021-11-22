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
import org.cafienne.actormodel.identity.{PlatformUser, UserIdentity}
import org.cafienne.humantask.actorapi.command.WorkflowCommand
import org.cafienne.infrastructure.akka.http.route.{CommandRoute, QueryRoute}
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.db.materializer.LastModifiedRegistration
import org.cafienne.service.db.query.exception.TaskSearchFailure
import org.cafienne.service.db.query.{CaseMembership, TaskQueries}

import scala.util.{Failure, Success}

trait TaskRoute extends CommandRoute with QueryRoute {
  val taskQueries: TaskQueries

  override val lastModifiedRegistration: LastModifiedRegistration = CaseReader.lastModifiedRegistration

  def askTaskWithMember(platformUser: PlatformUser, taskId: String, assignee: String, createTaskCommand: CreateTaskCommandWithAssignee): Route = {
    onComplete(taskQueries.getCaseMembership(taskId, platformUser)) {
      case Success(caseMember) => {
        val tenant = caseMember.tenant
        onComplete(userCache.getUsers(Seq(assignee), tenant)) {
          case Success(tenantUsers) => {
            if (tenantUsers.isEmpty) {
              // Not found, hence not a valid user (it can be also because the user account is not enabled)
              complete(StatusCodes.NotFound, s"Cannot find an active user '$assignee' in tenant '$tenant'")
            } else if (tenantUsers.size > 1) {
              logger.error(s"Found ${tenantUsers.size} users matching userId '$assignee' in tenant '$tenant'. The query should only result in one user only.")
              complete(StatusCodes.InternalServerError, s"An internal error happened while retrieving user information on user '$assignee'")
            } else {
              val member = tenantUsers.head
              askModelActor(createTaskCommand.apply(caseMember.caseInstanceId, caseMember, member))
            }
          }
          case Failure(t: Throwable) => {
            logger.warn(s"An error happened while retrieving user information on user '$assignee' in tenant '$tenant'", t)
            complete(StatusCodes.InternalServerError, s"An internal error happened while retrieving user information on user '$assignee'")
          }
        }
      }
      case Failure(error) => {
        error match {
          case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
          case _ => throw error
        }
      }
    }
  }

  def askTask(platformUser: PlatformUser, taskId: String, createTaskCommand: CreateTaskCommand): Route = {
    onComplete(taskQueries.getCaseMembership(taskId, platformUser)) {
      case Success(caseMember) => askModelActor(createTaskCommand.apply(caseMember.caseInstanceId, caseMember))
      case Failure(error) => {
        error match {
          case t: TaskSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
          case _ => throw error
        }
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
