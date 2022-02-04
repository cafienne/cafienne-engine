/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{complete, _}
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.cmmn.actorapi.command._
import org.cafienne.cmmn.actorapi.command.team._
import org.cafienne.cmmn.actorapi.command.team.setmember.SetCaseTeamUser
import org.cafienne.infrastructure.akkahttp.route.{CaseTeamValidator, CommandRoute, QueryRoute}
import org.cafienne.service.db.materializer.LastModifiedRegistration
import org.cafienne.service.db.materializer.cases.CaseReader
import org.cafienne.service.db.query.{CaseMembership, CaseQueries}
import org.cafienne.service.db.query.exception.CaseSearchFailure

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute with QueryRoute with CaseTeamValidator {
  override val lastModifiedRegistration: LastModifiedRegistration = CaseReader.lastModifiedRegistration
  val caseQueries: CaseQueries

  /**
    * Run the sub route with a valid platform user and case instance id
    *
    * @param subRoute
    * @return
    */
  def caseInstanceRoute(subRoute: (PlatformUser, String) => Route): Route = {
    validUser { platformUser =>
      path(Segment) { caseInstanceId =>
        pathEndOrSingleSlash {
          subRoute(platformUser, caseInstanceId)
        }
      }
    }
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    *
    * @param subRoute
    * @return
    */
  def caseInstanceSubRoute(subRoute: (PlatformUser, String) => Route): Route = {
    validUser { platformUser =>
      pathPrefix(Segment) { caseInstanceId =>
        subRoute(platformUser, caseInstanceId)
      }
    }
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    *
    * @param subRoute
    * @return
    */
  def caseInstanceSubRoute(prefix: String, subRoute: (PlatformUser, String) => Route): Route = {
    validUser { platformUser =>
      pathPrefix(Segment / prefix) { caseInstanceId =>
        subRoute(platformUser, caseInstanceId)
      }
    }
  }

  def putCaseTeamUser(platformUser: PlatformUser, caseInstanceId: String, newUser: CaseTeamUser): Route = {
    authorizeCaseAccess(platformUser, caseInstanceId, {
      member => {
        onComplete(userCache.getUserRegistration(newUser.userId)) {
          case Success(registeredPlatformUser) => askModelActor(new SetCaseTeamUser(member, caseInstanceId, newUser.copy(newOrigin = registeredPlatformUser.origin(member.tenant))))
          case Failure(t: Throwable) => complete(StatusCodes.NotFound, t.getLocalizedMessage)
        }
      }
    })
  }

  def authorizeCaseAccess(platformUser: PlatformUser, caseInstanceId: String, subRoute: CaseMembership => Route): Route = {
    readLastModifiedHeader() { caseLastModified =>
      onComplete(handleSyncedQuery(() => caseQueries.getCaseMembership(caseInstanceId, platformUser), caseLastModified)) {
        case Success(membership) => subRoute(membership)
        case Failure(error) =>
          error match {
            case t: CaseSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
            case _ => throw error
          }
      }
    }
  }

  def askCase(platformUser: PlatformUser, caseInstanceId: String, createCaseCommand: CaseMembership => CaseCommand): Route = {
    authorizeCaseAccess(platformUser, caseInstanceId, caseMember => askModelActor(createCaseCommand.apply(caseMember)))
  }
}
