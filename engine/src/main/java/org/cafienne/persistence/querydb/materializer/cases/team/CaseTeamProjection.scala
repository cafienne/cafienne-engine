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

package org.cafienne.persistence.querydb.materializer.cases.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.engine.cmmn.actorapi.event.team._
import org.cafienne.engine.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import org.cafienne.persistence.querydb.materializer.cases.{CaseEventBatch, CaseEventMaterializer}

class CaseTeamProjection(override val batch: CaseEventBatch) extends CaseEventMaterializer with LazyLogging {
  private val memberProjection = new CaseTeamMemberProjection(dBTransaction)
  private val deprecatedEventsProjection = new DeprecatedCaseTeamEventProjection(dBTransaction)

  def handleCaseTeamEvent(event: CaseTeamEvent): Unit = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    event match {
      case event: CaseTeamMemberEvent[_] => memberProjection.handleEvent(event)
      case event: DeprecatedCaseTeamEvent => deprecatedEventsProjection.handleDeprecatedCaseTeamEvent(event)
      case _ => ()// Ignore other events
    }
  }

  def prepareCommit(): Unit = {
    memberProjection.prepareCommit()
    deprecatedEventsProjection.prepareCommit()
  }
}
