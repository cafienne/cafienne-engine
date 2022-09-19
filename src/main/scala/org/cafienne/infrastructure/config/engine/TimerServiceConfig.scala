package org.cafienne.infrastructure.config.engine

import org.cafienne.infrastructure.config.util.ChildConfigReader

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

class TimerServiceConfig(val parent: EngineConfig) extends ChildConfigReader {
  def path = "timer-service"

  /**
    * Returns configuration path for the event store
    */
  lazy val store: String = {
    if (config.hasPath("store")) {
      readString("store")
    } else {
      val defaultValue = "akka-persistence-jdbc.shared-databases.slick"
      logger.warn("Event store configuration is missing, assuming default value " + defaultValue)
      defaultValue
    }
  }

  /**
    * Returns the duration of the window ahead to set timers in the in-memory scheduler
    */
  val window: FiniteDuration = {
    readDuration("window", FiniteDuration(65, TimeUnit.MINUTES))
  }

  /**
    * Returns the cycle time to refresh the timers loaded into the in-memory scheduler.
    * Cannot be longer than the duration given for the 'window'
    */
  val interval: FiniteDuration = {
    val interval = readDuration("interval", FiniteDuration(1, TimeUnit.HOURS))
    if (interval >= window) {
      fail(s"Timer service refresh interval (configured to $interval) must be shorter than the window ahead (which is configured to $window)")
    }
    interval
  }
}