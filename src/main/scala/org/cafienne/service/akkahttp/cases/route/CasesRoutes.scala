/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.infrastructure.akkahttp.route.AuthenticatedRoute
import org.cafienne.service.akkahttp.cases.route.deprecated.{DeprecatedCaseTeamRoute, DeprecatedPlanItemHistoryRoute}
import org.cafienne.system.CaseSystem

import javax.ws.rs._

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CasesRoutes(override val caseSystem: CaseSystem) extends AuthenticatedRoute {
  override val prefix = "cases"

  addSubRoute(new CaseRoute(caseSystem))
  addSubRoute(new CaseFileRoute(caseSystem))
  addSubRoute(new CaseTeamRoute(caseSystem))
  addSubRoute(new PlanItemRoute(caseSystem))
  addSubRoute(new DiscretionaryRoute(caseSystem))
  addSubRoute(new CaseDocumentationRoute(caseSystem))
  addSubRoute(new CaseHistoryRoute(caseSystem))
  addSubRoute(new CaseMigrationRoute(caseSystem))
  addSubRoute(new DeprecatedPlanItemHistoryRoute(caseSystem))
  addSubRoute(new DeprecatedCaseTeamRoute(caseSystem))
}
