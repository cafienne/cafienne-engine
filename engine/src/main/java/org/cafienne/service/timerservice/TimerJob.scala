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

package org.cafienne.service.timerservice

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.{Cancellable, Scheduler}
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.engine.actorapi.CaseFamily
import org.cafienne.engine.cmmn.actorapi.command.plan.eventlistener.RaiseEvent

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

class TimerJob(val timerService: TimerService, val timer: Timer, val scheduler: Scheduler) extends Runnable with LazyLogging {
  val command = new RaiseEvent(timer.user, timer.caseInstanceId, timer.timerId)
  private val millis: Long = timer.moment.toEpochMilli
  private val delay: Long = millis - System.currentTimeMillis
  private val responseTracker = new ResponseTracker
  private var count = 0L

  val duration: FiniteDuration = Duration.create(delay, TimeUnit.MILLISECONDS)
  logger.whenDebugEnabled(logger.debug(s"Scheduling to run timer request ${timer.timerId} in ${duration.length / 1000}.${duration.length % 1000} seconds from now (at ${timer.moment})"))
  val schedule: Cancellable = scheduler.scheduleOnce(duration, this)

  def run(): Unit = {
    // This is the first time the timer goes off. We're sending raiseEvent, and start an additional internal timer.
    //  this internal timer tracks whether a response is gotten back from the case instance.
    //  if this is not received within 10 seconds, it tries again, up to 10 times.
    invokeTimer()
  }

  private def invokeTimer(): Unit = {
    count += 1
    logger.whenDebugEnabled(logger.debug(s"Raising timer in case ${timer.caseInstanceId} for timer ${timer.timerId} on behalf of user ${timer.userId}"))
    if (count > 1) {
      logger.warn(s"Attempt number $count to raise timer $timer in case ${timer.caseInstanceId}.")
    }
    timerService.caseSystem.engine.inform(new CaseFamily(timer.rootCaseId), command, timerService.self);
    responseTracker.start()
  }

  def cancel(): Boolean = {
    responseTracker.stop()
    schedule.cancel()
  }

  def handleResponse(response: ModelResponse): Unit = {
    responseTracker.stop()
    response match {
      case failure: CommandFailure =>
        logger.warn(s"Could not trigger timer $timer in case ${timer.caseInstanceId}:" + failure.exception())
      case _ => // nothing to do, the response tracker is already stopped.
    }
  }

  private class ResponseTracker extends Runnable {
    private var retry: Option[Cancellable] = None

    def run(): Unit = {
      invokeTimer()
    }

    def start(): Unit = {
      if (count > 10) {
        logger.warn(s"Tried 10 attempts to trigger timer $timer in case ${timer.caseInstanceId}, without getting a response. Timer-retry mechanism is canceled.")
      } else {
        stop() // First stop any current schedules
        retry = Some(scheduler.scheduleOnce(Duration.create(10, TimeUnit.SECONDS), this))
      }
    }
    def stop(): Unit = {
      retry.foreach(_.cancel());
    }
  }
}
