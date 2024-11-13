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

package com.casefabric.infrastructure.cqrs.batch

import com.casefabric.actormodel.event.CommitEvent
import com.casefabric.infrastructure.cqrs.ModelEventEnvelope

import scala.collection.mutable.ListBuffer

trait EventBatch {
  val persistenceId: String

  val events = new ListBuffer[ModelEventEnvelope]()

  lazy val commitEvent: CommitEvent = events.last.event.asInstanceOf[CommitEvent]

  def addEvent(envelope: ModelEventEnvelope): Unit = {
    events += envelope
  }
}
