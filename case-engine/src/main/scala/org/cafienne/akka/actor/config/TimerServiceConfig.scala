package org.cafienne.akka.actor.config

class TimerServiceConfig(val parent: EngineConfig) extends CafienneBaseConfig {
  val path = "timer-service"

  lazy val persistDelay: Long = {
    val default = 60
    val period = readLong("persist-delay", default)
    logger.info("Timer service will persist snapshot changes every " + period + " seconds")
    period
  }
}