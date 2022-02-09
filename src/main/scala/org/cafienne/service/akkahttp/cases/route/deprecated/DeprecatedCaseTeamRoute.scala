/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route.deprecated

import akka.http.scaladsl.server.Directives.{path, _}
import akka.http.scaladsl.server.Route
import org.cafienne.cmmn.actorapi.command.team.DeprecatedUpsert
import org.cafienne.cmmn.actorapi.command.team.removemember.{RemoveCaseTeamTenantRole, RemoveCaseTeamUser}
import org.cafienne.querydb.query.{CaseQueries, CaseQueriesImpl}
import org.cafienne.service.akkahttp.cases.model.CaseTeamAPI.Compatible._
import org.cafienne.service.akkahttp.cases.route.CasesRoute
import org.cafienne.system.CaseSystem

class DeprecatedCaseTeamRoute(override val caseSystem: CaseSystem) extends CasesRoute {

  val caseQueries: CaseQueries = new CaseQueriesImpl
  override val addToSwaggerRoutes = false
  override def routes: Route = concat(putCaseTeamMember, deleteCaseTeamMember)

  def putCaseTeamMember: Route = put {
    caseInstanceSubRoute { (platformUser, caseInstanceId) => {
      path("caseteam") {
        entity(as[BackwardCompatibleTeamMemberFormat]) { input =>
          askCase(platformUser, caseInstanceId, caseOwner => new DeprecatedUpsert(caseOwner, caseInstanceId, input.upsertMemberData))
        }
      }
    }
    }
  }

  def deleteCaseTeamMember: Route = delete {
    caseInstanceSubRoute { (platformUser, caseInstanceId) =>
      path("caseteam" / Segment) { memberId =>
        parameters("type".?) { memberType =>
          if (memberType.nonEmpty && memberType.get == "role") {
            askCase(platformUser, caseInstanceId, tenantUser => new RemoveCaseTeamTenantRole(tenantUser, caseInstanceId, memberId))
          } else {
            askCase(platformUser, caseInstanceId, tenantUser => new RemoveCaseTeamUser(tenantUser, caseInstanceId, memberId))
          }
        }
      }
    }
  }
}
