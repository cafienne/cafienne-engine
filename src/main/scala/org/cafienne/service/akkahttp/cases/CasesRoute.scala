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

package org.cafienne.service.akkahttp.cases

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.authentication.AuthenticatedUser
import org.cafienne.cmmn.actorapi.command._
import org.cafienne.infrastructure.akkahttp.route.{CommandRoute, QueryRoute}
import org.cafienne.querydb.query.exception.CaseSearchFailure
import org.cafienne.querydb.query.{CaseMembership, CaseQueries, CaseQueriesImpl}
import org.cafienne.service.akkahttp.Headers

import scala.util.{Failure, Success}

trait CasesRoute extends CommandRoute with QueryRoute {
  val caseQueries: CaseQueries = new CaseQueriesImpl
  override val lastModifiedHeaderName: String = Headers.CASE_LAST_MODIFIED

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
