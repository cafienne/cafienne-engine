/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route.deprecated

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.cafienne.service.akkahttp.cases.route.CasesRoute
import org.cafienne.system.CaseSystem

class DeprecatedPlanItemHistoryRoute(override val caseSystem: CaseSystem) extends CasesRoute {
  override val addToSwaggerRoutes = false
  override def routes: Route = concat(deprecatedPlanItemHistory)

  def deprecatedPlanItemHistory: Route = get {
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