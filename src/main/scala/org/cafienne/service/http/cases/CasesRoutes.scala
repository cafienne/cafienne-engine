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

package org.cafienne.service.http.cases

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.cafienne.infrastructure.http.route.AuthenticatedRoute
import org.cafienne.service.http.cases.deprecated.{DeprecatedCaseTeamRoute, DeprecatedPlanItemHistoryRoute}
import org.cafienne.service.http.cases.documentation.CaseDocumentationRoute
import org.cafienne.service.http.cases.file.CaseFileRoute
import org.cafienne.service.http.cases.history.CaseHistoryRoute
import org.cafienne.service.http.cases.migration.CaseMigrationRoute
import org.cafienne.service.http.cases.plan.{DiscretionaryRoute, PlanItemRoute}
import org.cafienne.service.http.cases.team.CaseTeamRoute
import org.cafienne.system.CaseSystem

import jakarta.ws.rs._

@SecurityRequirement(name = "oauth2", scopes = Array("openid"))
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
