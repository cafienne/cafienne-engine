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

package org.cafienne.timerservice.persistence

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.timerservice.Timer

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

/**
  * Storage for timer jobs
  */
trait TimerStore extends LazyLogging {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  def getOffset: Future[Offset]

  val storageName: String = "Timer Service Offset"

  def importTimers(list: Seq[Timer]): Unit

  def getTimers(window: Instant): Future[Seq[Timer]]

  def storeTimer(job: Timer, offset: Option[Offset]): Future[Done]

  def removeTimer(timerId: String, offset: Option[Offset]): Future[Done]

  /**
    * Return a description for this type of timer store, defaults to simple class name
    */
  lazy val description: String = getClass.getSimpleName
}
