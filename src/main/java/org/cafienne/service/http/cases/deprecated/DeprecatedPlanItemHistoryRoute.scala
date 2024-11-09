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

package org.cafienne.service.http.cases.deprecated

import org.apache.pekko.http.scaladsl.server.Route
import org.cafienne.service.http.cases.history.CaseEventsBaseRoute
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
            case Success(value) => completeJson(value)
            case Failure(t) => handleFailure(t)
          }
        }
      }
    }
  }
}