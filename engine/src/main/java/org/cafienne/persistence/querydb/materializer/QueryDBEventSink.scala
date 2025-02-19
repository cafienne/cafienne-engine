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

package org.cafienne.persistence.querydb.materializer

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.Done
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.RestartSettings
import org.apache.pekko.stream.scaladsl.Sink
import org.cafienne.infrastructure.cqrs.batch.EventBatchSource
import org.cafienne.system.CaseSystem
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait QueryDBEventSink extends EventBatchSource[QueryDBEventBatch] with LazyLogging {
  val caseSystem: CaseSystem // Need to provide a CaseSystem

  override def system: ActorSystem = caseSystem.system
  override val readJournal: String = caseSystem.config.persistence.readJournal
  override val restartSettings: RestartSettings = caseSystem.config.persistence.queryDB.restartSettings


  import scala.concurrent.ExecutionContext.Implicits.global

  /**
    * Start reading and processing events
    */
  def start(): Unit = {
    batches
      .mapAsync(1)(batch => {
        batch.consume()
        Future.successful(Done)
      }) // Now handle the batch (would be better if that is done through a real Sink, not yet sure how to achieve that - make EventBatch extend Sink???)
      .runWith(Sink.ignore)
      .onComplete {
        case Success(_) => //
        case Failure(ex) => reportUnhealthy(ex)
      }
  }

  def reportUnhealthy(throwable: Throwable): Unit = {
    // No need to print the stack trace itself here, as that is done in HealthMonitor as well.
    logger.error(s"${getClass.getSimpleName} bumped into an issue that it cannot recover from: ${throwable.getMessage}")
    HealthMonitor.readJournal.hasFailed(throwable)
  }
}
