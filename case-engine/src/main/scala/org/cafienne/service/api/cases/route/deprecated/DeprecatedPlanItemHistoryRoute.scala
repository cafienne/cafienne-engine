/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route.deprecated

import akka.http.scaladsl.server.Directives._
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.service.api.cases.route.CasesRoute
import org.cafienne.service.db.query.CaseQueries
import org.cafienne.system.CaseSystem

class DeprecatedPlanItemHistoryRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider, override implicit val caseSystem: CaseSystem) extends CasesRoute {
  override val addToSwaggerRoutes = false
  override def routes = concat(deprecatedPlanItemHistory)

  def deprecatedPlanItemHistory = get {
    validUser { platformUser =>
      path(Segment / "planitems" / Segment / "history") {
        (caseInstanceId, planItemId) => {
          extractUri { uri =>
            logger.warn(s"Using deprecated API to get plan item history:")
            logger.warn(s"Old: /$caseInstanceId/planitems/$planItemId/history")
            logger.warn(s"New: /$caseInstanceId/history/planitems/$planItemId")
            runQuery(caseQueries.getPlanItemHistory(planItemId, platformUser))
          }
        }
      }
    }
  }
}