package org.cafienne.timerservice

import akka.actor.{Cancellable, Scheduler}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

class TimerJob(val timerService: TimerService, val timer: Timer, val scheduler: Scheduler) extends Runnable with LazyLogging {
  val command = new RaiseEvent(timer.user, timer.caseInstanceId, timer.timerId)
  val millis: Long = timer.moment.toEpochMilli
  val delay: Long = millis - System.currentTimeMillis

  val duration: FiniteDuration = Duration.create(delay, TimeUnit.MILLISECONDS)
  logger.whenDebugEnabled(logger.debug(s"Scheduling to run timer request ${timer.timerId} in ${duration.length / 1000}.${duration.length % 1000} seconds from now (at ${timer.moment})"))
  val schedule: Cancellable = scheduler.scheduleOnce(duration, this)

  def run(): Unit = {
    logger.whenDebugEnabled(logger.debug(s"Raising timer in case ${timer.caseInstanceId} for timer ${timer.timerId} on behalf of user ${timer.userId}"))
    timerService.askCase(command, handleFailingCaseInvocation, handleCaseInvocation)
  }

  def cancel(): Boolean = {
    schedule.cancel()
  }

  def handleFailingCaseInvocation(failure: CommandFailure): Unit = {
    // TODO: we can also update the timer state in the storage???
    logger.warn(s"Could not trigger timer $timer in case ${timer.caseInstanceId}:" + failure.toJson)
  }

  def handleCaseInvocation(response: ModelResponse): Unit = {
    // TODO: we can also delete the timer here, or update a state for that timer in the store
    logger.whenDebugEnabled(logger.debug(s"Successfully invoked timer $timer in case ${timer.caseInstanceId}"))
  }
}
