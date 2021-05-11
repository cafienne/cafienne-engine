package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.ChildConfigReader

class TimerServiceConfig(val parent: EngineConfig) extends ChildConfigReader {
  val path = "timer-service"

  lazy val persistDelay: Long = {
    val default = 60
    val period = readLong("persist-delay", default)
    logger.info("Timer service will persist snapshot changes every " + period + " seconds")
    period
  }

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
}