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

package org.cafienne.timerservice.persistence.inmemory

import org.apache.pekko.Done
import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.timerservice.Timer
import org.cafienne.timerservice.persistence.TimerStore

import java.time.Instant
import scala.collection.mutable
import scala.concurrent.Future

class InMemoryStore extends TimerStore with LazyLogging {
  val timers = new mutable.HashMap[String, Timer]()

  override def getOffset: Future[Offset] = Future.successful(Offset.noOffset)

  override def getTimers(window: Instant): Future[Seq[Timer]] = {
    Future.successful(timers.values.filter(_.moment.toEpochMilli <= window.toEpochMilli).toSeq)
  }

  override def storeTimer(job: Timer, offset: Option[Offset]): Future[Done] = {
    timers.put(job.timerId, job)
    logger.debug(s"Stored timer $job")
    Future.successful(Done)
  }

  override def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    timers.remove(timerId)
    logger.debug(s"Removed timer $timerId")
    Future.successful(Done)
  }

  override def importTimers(list: Seq[Timer]): Unit = {
    list.foreach(timer => timers.put(timer.timerId, timer))
  }
}
