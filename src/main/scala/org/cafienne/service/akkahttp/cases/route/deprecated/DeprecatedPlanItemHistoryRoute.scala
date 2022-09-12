/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.akkahttp.cases.route.deprecated

import akka.http.scaladsl.server.Route
import org.cafienne.service.akkahttp.cases.route.CaseEventsBaseRoute
import org.cafienne.system.CaseSystem

import scala.util.{Failure, Success}

class DeprecatedPlanItemHistoryRoute(override val caseSystem: CaseSystem) extends CaseEventsBaseRoute {
  override val addToSwaggerRoutes = false
  override def routes: Route = concat(deprecatedPlanItemHistory)

  def deprecatedPlanItemHistory: Route = get {
    caseEventsSubRoute { caseEvents =>
      path("history" / "planitems" / Segment) { planItemId =>
        extractUri { uri =>
          logger.warn(s"Using deprecated API to get plan item history:")
          logger.warn(s"Old: /${caseEvents.caseInstanceId}/planitems/$planItemId/history")
          logger.warn(s"New: /${caseEvents.caseInstanceId}/history/planitems/$planItemId")

          onComplete(caseEvents.planitemHistory(planItemId)) {
            case Success(value) => completeJsonValue(value.toValue)
            case Failure(t) => handleFailure(t)
          }
        }
      }
    }
  }
}