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

package com.casefabric.querydb.materializer.cases.team

import org.apache.pekko.Done
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.cmmn.actorapi.event.team._
import com.casefabric.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import com.casefabric.querydb.materializer.cases.{CaseEventBatch, CaseEventMaterializer}

import scala.concurrent.{ExecutionContext, Future}

class CaseTeamProjection(override val batch: CaseEventBatch)(implicit val executionContext: ExecutionContext) extends CaseEventMaterializer with LazyLogging {
  private val memberProjection = new CaseTeamMemberProjection(dBTransaction)
  private val deprecatedEventsProjection = new DeprecatedCaseTeamEventProjection(dBTransaction)

  def handleCaseTeamEvent(event: CaseTeamEvent): Future[Done] = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    event match {
      case event: CaseTeamMemberEvent[_] => memberProjection.handleEvent(event)
      case event: DeprecatedCaseTeamEvent => deprecatedEventsProjection.handleDeprecatedCaseTeamEvent(event)
      case _ => // Ignore other events
    }
    Future.successful(Done)
  }

  def prepareCommit(): Unit = {
    memberProjection.prepareCommit()
    deprecatedEventsProjection.prepareCommit()
  }
}
