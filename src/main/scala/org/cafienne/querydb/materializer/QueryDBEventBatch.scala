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

package org.cafienne.querydb.materializer

import org.apache.pekko.Done
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.batch.EventBatch

import scala.concurrent.Future

trait QueryDBEventBatch extends EventBatch {

  def handleEvent(envelope: ModelEventEnvelope): Future[Done]

  def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done]

  def consume(): Future[Done] = {
    import scala.concurrent.ExecutionContext.Implicits.global

    // Create a chain of futures to be executed on the events
    //  Note, this can be improved if specific transactions handle the events in a smarter manner (e.g. all plan item events with the same id in a single shot instead of consecutively)
    var orderedEventHandling: Future[Done] = Future { Done }
    for (event <- events) {
      orderedEventHandling = orderedEventHandling.flatMap { _ => handleEvent(event) }
    }

    orderedEventHandling.flatMap(_ => commit(events.last, commitEvent))
  }
}
