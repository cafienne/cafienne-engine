package org.cafienne.timerservice

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.Cafienne
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
    storage.getTimers(nextWindow).map(timers => {
      logger.whenDebugEnabled(logger.debug(s"Storage returned ${timers.length} timers to plan in window $nextWindow"))
      timers.foreach(schedule.scheduleTimer)
    })
  }

  def fitsActiveWindow(timer: Timer): Boolean = {
    logger.whenDebugEnabled(logger.debug(s"Checking if timer $timer fits current window gives - ${timer.moment.toEpochMilli < activeWindow}"))
    timer.moment.toEpochMilli < activeWindow
  }
}
