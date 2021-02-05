/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives._
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.CaseQueries

class DeprecatedPlanItemHistoryRoute(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute {
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