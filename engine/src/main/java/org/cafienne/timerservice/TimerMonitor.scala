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
import org.apache.pekko.Done
import org.apache.pekko.actor.Scheduler
import org.apache.pekko.persistence.query.Offset
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.cmmn.actorapi.event.plan.eventlistener.TimerSet
import org.cafienne.system.health.HealthMonitor

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

class TimerMonitor(val timerService: TimerService) extends LazyLogging {
  private val activeTimers: mutable.Map[String, TimerJob] = mutable.Map()
  implicit val dispatcher: ExecutionContext = timerService.caseSystem.system.dispatcher
  val scheduler: Scheduler = timerService.caseSystem.system.scheduler
  val reader = new TimerStorageReader(this)

  def start(): Unit = {
    // Start off with immediately getting first set of timers from storage ...
    //  Note: this loads existing timers faster than first scheduling it.
    reader.loadNextWindow

    // ... and then schedule the reader to repeat itself at the configured interval
    val interval: FiniteDuration = timerService.caseSystem.config.engine.timerService.interval
    scheduler.scheduleWithFixedDelay(interval, interval)(reader)
  }

  def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    activeTimers.remove(timerId).map(schedule => schedule.cancel())
    runStorage(timerService.storage.removeTimer(timerId, offset))
  }

  def handleResponseMessage(response: ModelResponse): Unit = {
    activeTimers.values.filter(timer => timer.command.getMessageId eq response.getMessageId).foreach(_.handleResponse(response))
  }

  def runStorage(function: => Future[Done]): Future[Done] = {
    try {
      val result = function
      HealthMonitor.timerService.isOK()
      result
    } catch {
      case t: Throwable =>
        HealthMonitor.timerService.hasFailed(t)
        Future.successful(Done)
    }
  }

  def addTimer(event: TimerSet, offset: Offset): Future[Done] = {
    val job: Timer = Timer(event.getCaseInstanceId, event.getTimerId, event.getTargetMoment, event.getUser.id)
    // If the timer fits the current window ahead, immediately schedule it. Otherwise just persist.
    if (reader.fitsActiveWindow(job)) {
      scheduleTimer(job)
    }
    runStorage(timerService.storage.storeTimer(job, Some(offset)))
  }

  def scheduleTimer(timer: Timer): Unit = {
    activeTimers.getOrElseUpdate(timer.timerId, new TimerJob(timerService, timer, scheduler))
  }
}
