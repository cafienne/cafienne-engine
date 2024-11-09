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

package org.cafienne.infrastructure.cqrs.batch

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.infrastructure.cqrs.batch.public_events.PublicCaseEventBatch

trait PublicCaseEventBatchSource extends EventBatchSource[PublicCaseEventBatch] with LazyLogging {
  // PublicCaseEventBatch works on CaseEvents only
  override val tag: String = CaseEvent.TAG

  /**
    * This method must be implemented by the consumer to handle the new batch of ModelEvents
    * When a CommitEvent is encountered, the batch is considered complete.
    * The source handler will then invoke the [[EventBatch#consume()]] method
    * and a next batch will be created when new events come in.
    *
    * @param persistenceId The id of the ModelActor that produced the batch of events
    */
  override def createBatch(persistenceId: String): PublicCaseEventBatch = new PublicCaseEventBatch(persistenceId)

  def publicEvents: Source[PublicCaseEventBatch, NotUsed] = {
    // Note: we're filtering out empty batches! These occur e.g. when a subcase is started and it reports
    //  back to the main case about successful start. This does not lead to any public events on the case.
    batches.map(_.createPublicEvents).filter(_.publicEvents.nonEmpty)
  }
}
