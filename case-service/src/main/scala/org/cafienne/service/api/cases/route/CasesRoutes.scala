/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives._
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.CaseQueries

import scala.collection.immutable.Seq

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CasesRoutes(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {

  val caseRoute = new CaseRoute(caseQueries)(userCache)
  val caseFileRoute = new CaseFileRoute(caseQueries)(userCache)
  val caseTeamRoute = new CaseTeamRoute(caseQueries)(userCache)
  val casePlanRoute = new PlanItemRoute(caseQueries)(userCache)
  val discretionaryRoute = new DiscretionaryRoute(caseQueries)(userCache)
  val caseDocumentationRoute = new CaseDocumentationRoute(caseQueries)(userCache)
  val caseHistoryRoute = new CaseHistoryRoute(caseQueries)(userCache)
  val deprecatedPlanItemHistoryRoute = new DeprecatedPlanItemHistoryRoute(caseQueries)(userCache)

  override def routes =
    pathPrefix("cases") {
      caseRoute.routes ~
      caseFileRoute.routes ~
      casePlanRoute.routes ~
      caseTeamRoute.routes ~
      discretionaryRoute.routes ~
      caseDocumentationRoute.routes ~
      caseHistoryRoute.routes ~
      deprecatedPlanItemHistoryRoute.routes
    }

  override def apiClasses(): Seq[Class[_]] = {
    Seq(classOf[CaseRoute], classOf[CaseFileRoute], classOf[CaseTeamRoute], classOf[PlanItemRoute], classOf[DiscretionaryRoute], classOf[CaseDocumentationRoute], classOf[CaseHistoryRoute])
  }
}
