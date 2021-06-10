package org.cafienne.actormodel.config

import org.cafienne.actormodel.config.util.ChildConfigReader

class TimerServiceConfig(val parent: EngineConfig) extends ChildConfigReader {
  val path = "timer-service"

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