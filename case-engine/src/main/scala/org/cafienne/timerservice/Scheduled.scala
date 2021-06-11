package org.cafienne.timerservice

import akka.actor.Cancellable
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent
import org.cafienne.system.CaseSystem

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}

class Scheduled(val timerService: TimerService, val timer: Timer, val sink: TimerEventSink = null)(implicit val caseSystem: CaseSystem) extends LazyLogging {

  val command = new RaiseEvent(timer.user, timer.caseInstanceId, timer.timerId)
  val millis: Long = timer.moment.toEpochMilli
  val delay: Long = millis - System.currentTimeMillis

  val duration: FiniteDuration = Duration.create(delay, TimeUnit.MILLISECONDS)
  logger.whenDebugEnabled(logger.debug(s"Scheduling to run timer request ${timer.timerId} in ${duration.length / 1000}.${duration.length % 1000} seconds from now (at ${timer.moment})"))

  val schedule: Cancellable = caseSystem.system.scheduler.scheduleOnce(duration, () => {
    logger.whenDebugEnabled(logger.debug(s"Raising timer in case ${timer.caseInstanceId} for timer ${timer.timerId} on behalf of user ${timer.user.id}"))
    sink.timerService.askCase(command,
      failure => sink.handleFailingCaseInvocation(this, failure),
      success => sink.handleCaseInvocation(this, success))
  })

  def cancel(): Boolean = {
    schedule.cancel()
  }
}
