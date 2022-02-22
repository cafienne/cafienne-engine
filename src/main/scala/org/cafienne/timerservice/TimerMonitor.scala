package org.cafienne.timerservice

import akka.Done
import akka.actor.Scheduler
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.cmmn.actorapi.event.plan.eventlistener.TimerSet

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class TimerMonitor(val timerService: TimerService) extends LazyLogging {
  private val activeTimers: mutable.Map[String, TimerJob] = mutable.Map()
  implicit val dispatcher: ExecutionContext = timerService.caseSystem.system.dispatcher
  val scheduler: Scheduler = timerService.caseSystem.system.scheduler

  def start(): Future[Unit] = {
    logger.warn("Loading timers from storage")
    loadTimers()
  }

  def loadTimers(): Future[Unit] = {
    timerService.storage.getTimers().map(timers => {
      logger.info(s"Scheduling batch with ${timers.length} timers upon startup.")
      timers.foreach(scheduleTimer)
    })
  }

  def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    activeTimers.remove(timerId).map(schedule => schedule.cancel())
    timerService.storage.removeTimer(timerId, offset)
  }

  def setTimer(event: TimerSet, offset: Offset): Future[Done] = {
    val job: Timer = Timer(event.getCaseInstanceId, event.getTimerId, event.getTargetMoment, event.getUser.id)
    scheduleTimer(job)
    timerService.storage.storeTimer(job, Some(offset))
  }

  private def scheduleTimer(job: Timer): Unit = {
    activeTimers.put(job.timerId, new TimerJob(timerService, job, scheduler))
  }

  def handleFailingCaseInvocation(job: TimerJob, failure: CommandFailure): Unit = {
    // TODO: we can also update the timer state in the storage???
    logger.warn(s"Could not trigger timer ${job.timer.timerId} in case ${job.timer.caseInstanceId}:" + failure.toJson)
  }

  def handleCaseInvocation(job: TimerJob, response: ModelResponse): Unit = {
    // TODO: we can also delete the timer here, or update a state for that timer in the store
    logger.whenDebugEnabled(logger.debug(s"Successfully invoked timer ${job.timer.timerId} in case ${job.timer.caseInstanceId}"))
  }
}
