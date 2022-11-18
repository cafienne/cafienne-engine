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

package org.cafienne.timerservice

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
import org.cafienne.system.health.HealthMonitor
import org.cafienne.timerservice.persistence.TimerStore

import java.time.Instant
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class TimerStorageReader(schedule: TimerMonitor) extends Runnable with LazyLogging {
  implicit val dispatcher: ExecutionContext = schedule.timerService.caseSystem.system.dispatcher
  private val storage: TimerStore = schedule.timerService.storage
  private val window: FiniteDuration = Cafienne.config.engine.timerService.window
  private var activeWindow: Long = -1

  override def run(): Unit = loadNextWindow

  def loadNextWindow: Future[Unit] = {
    val nextWindow = Instant.now().plusMillis(window.toMillis)
    activeWindow = nextWindow.toEpochMilli
    logger.whenDebugEnabled(logger.debug(s"Reading timers from TimerStore for next $window (setting active window to $nextWindow)"))

    val timers = {
      try {
        val result = storage.getTimers(nextWindow)
        HealthMonitor.timerService.isOK()
        result
      } catch {
        case t: Throwable =>
          HealthMonitor.timerService.hasFailed(t)
          Future.successful(Seq())
      }
    }
    timers.map(_.foreach(schedule.scheduleTimer))
  }

  def fitsActiveWindow(timer: Timer): Boolean = {
    logger.whenDebugEnabled(logger.debug(s"Checking if timer $timer fits current window gives - ${timer.moment.toEpochMilli < activeWindow}"))
    timer.moment.toEpochMilli < activeWindow
  }
}
