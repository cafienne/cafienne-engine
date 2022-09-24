/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.cmmn.actorapi.command._
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.materializer.LastModifiedRegistration
import org.cafienne.querydb.materializer.cases.CaseReader
import org.cafienne.querydb.query.exception.CaseSearchFailure
import org.cafienne.querydb.query.{CaseMembership, CaseQueries, CaseQueriesImpl}

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute with QueryRoute {
  override val lastModifiedRegistration: LastModifiedRegistration = CaseReader.lastModifiedRegistration
  val caseQueries: CaseQueries = new CaseQueriesImpl

  def caseUser(subRoute: AuthenticatedUser => Route): Route = {
    super.authenticatedUser(subRoute)
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    *
    * @param subRoute
    * @return
    */
  def caseInstanceRoute(subRoute: (UserIdentity, String) => Route): Route = {
    caseUser { user =>
      path(Segment) { caseInstanceId =>
        pathEndOrSingleSlash {
          subRoute(user, caseInstanceId)
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
  def caseInstanceSubRoute(subRoute: (UserIdentity, String) => Route): Route = {
    caseUser { user =>
      pathPrefix(Segment) { caseInstanceId =>
        subRoute(user, caseInstanceId)
      }
    }
  }

  /**
    * Run the sub route with a valid platform user and case instance id
    *
    * @param subRoute
    * @return
    */
  def caseInstanceSubRoute(prefix: String)(subRoute: (UserIdentity, String) => Route): Route = {
    caseUser { user =>
      pathPrefix(Segment / prefix) { caseInstanceId =>
        subRoute(user, caseInstanceId)
      }
    }
  }

  def authorizeCaseAccess(user: UserIdentity, caseInstanceId: String, subRoute: CaseMembership => Route): Route = {
    readLastModifiedHeader() { caseLastModified =>
      onComplete(runSyncedQuery(caseQueries.getCaseMembership(caseInstanceId, user), caseLastModified)) {
        case Success(membership) => subRoute(membership)
        case Failure(error) =>
          error match {
            case t: CaseSearchFailure => complete(StatusCodes.NotFound, t.getLocalizedMessage)
            case _ => throw error
          }
      }
    }
  }

  def askCase(user: UserIdentity, caseInstanceId: String, createCaseCommand: CaseMembership => CaseCommand): Route = {
    authorizeCaseAccess(user, caseInstanceId, caseMember => askModelActor(createCaseCommand.apply(caseMember)))
  }
}
