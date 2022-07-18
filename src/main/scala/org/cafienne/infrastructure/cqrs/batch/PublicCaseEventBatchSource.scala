/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch

import akka.NotUsed
import akka.stream.scaladsl.Source
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

  def publicEvents: Source[PublicCaseEventBatch, NotUsed] = batches.map(_.createPublicEvents)
}
