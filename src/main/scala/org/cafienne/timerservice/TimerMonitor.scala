package org.cafienne.timerservice

import akka.Done
import akka.actor.Scheduler
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.plan.eventlistener.TimerSet
import org.cafienne.infrastructure.Cafienne

import java.util.concurrent.TimeUnit
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
    val interval: FiniteDuration = Cafienne.config.engine.timerService.interval
    scheduler.scheduleAtFixedRate(interval, interval)(reader)
  }

  def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    activeTimers.remove(timerId).map(schedule => schedule.cancel())
    timerService.storage.removeTimer(timerId, offset)
  }

  def addTimer(event: TimerSet, offset: Offset): Future[Done] = {
    val job: Timer = Timer(event.getCaseInstanceId, event.getTimerId, event.getTargetMoment, event.getUser.id)
    // If the timer fits the current window ahead, immediately schedule it. Otherwise just persist.
    if (reader.fitsActiveWindow(job)) {
      scheduleTimer(job)
    }
    timerService.storage.storeTimer(job, Some(offset))
  }

  def scheduleTimer(timer: Timer): Unit = {
    activeTimers.getOrElseUpdate(timer.timerId, new TimerJob(timerService, timer, scheduler))
  }
}
