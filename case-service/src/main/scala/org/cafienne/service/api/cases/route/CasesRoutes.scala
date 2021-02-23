/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import javax.ws.rs._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.CaseQueries

@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CasesRoutes(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {
  override val prefix = "cases"

  addSubRoute(new CaseRoute(caseQueries)(userCache))
  addSubRoute(new CaseFileRoute(caseQueries)(userCache))
  addSubRoute(new CaseTeamRoute(caseQueries)(userCache))
  addSubRoute(new PlanItemRoute(caseQueries)(userCache))
  addSubRoute(new DiscretionaryRoute(caseQueries)(userCache))
  addSubRoute(new CaseDocumentationRoute(caseQueries)(userCache))
  addSubRoute(new CaseHistoryRoute(caseQueries)(userCache))
  addSubRoute(new DeprecatedPlanItemHistoryRoute(caseQueries)(userCache))
}
