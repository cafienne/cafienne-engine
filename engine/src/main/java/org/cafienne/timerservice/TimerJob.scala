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
import org.apache.pekko.actor.{Cancellable, Scheduler}
import org.cafienne.actormodel.communication.outgoing.RemoteActorState
import org.cafienne.actormodel.communication.outgoing.response.ActorRequestFailed
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

class TimerJob(val timerService: TimerService, val timer: Timer, val scheduler: Scheduler) extends Runnable with LazyLogging {
  val command = new RaiseEvent(timer.user, timer.caseInstanceId, timer.timerId)
  val state: TimerItemState = new TimerItemState()
  val millis: Long = timer.moment.toEpochMilli
  val delay: Long = millis - System.currentTimeMillis

  val duration: FiniteDuration = Duration.create(delay, TimeUnit.MILLISECONDS)
  logger.whenDebugEnabled(logger.debug(s"Scheduling to run timer request ${timer.timerId} in ${duration.length / 1000}.${duration.length % 1000} seconds from now (at ${timer.moment})"))
  val schedule: Cancellable = scheduler.scheduleOnce(duration, this)

  def run(): Unit = {
    logger.whenDebugEnabled(logger.debug(s"Raising timer in case ${timer.caseInstanceId} for timer ${timer.timerId} on behalf of user ${timer.userId}"))
    state.raiseTimer()
  }

  def cancel(): Boolean = {
    schedule.cancel()
  }

  class TimerItemState extends RemoteActorState[TimerService](timerService, timer.caseInstanceId) {
    def raiseTimer(): Unit = {
      sendRequest(command)
    }

    override def handleFailure(failure: ActorRequestFailed): Unit = {
      // TODO: we can also update the timer state in the storage???
      logger.warn(s"Could not trigger timer $timer in case ${timer.caseInstanceId}:" + failure.exceptionAsJSON)
    }
  }
}
