/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.service.api.cases.route.deprecated.{DeprecatedCaseTeamRoute, DeprecatedPlanItemHistoryRoute}
import org.cafienne.service.db.query.CaseQueries
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CasesRoutes(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {
  override val prefix = "cases"

  addSubRoute(new CaseRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new CaseFileRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new CaseTeamRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new PlanItemRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new DiscretionaryRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new CaseDocumentationRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new CaseHistoryRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new CaseMigrationRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new DeprecatedPlanItemHistoryRoute(caseQueries)(userCache, caseSystem))
  addSubRoute(new DeprecatedCaseTeamRoute(caseQueries)(userCache, caseSystem))
}
