package org.cafienne.akka.actor.config

class PlatformServiceConfig(val parent: EngineConfig) extends CafienneBaseConfig {
  val path = "platform-service"

  lazy val workers: Int = {
    val default = 5
    val numWorkers = readInt("workers", default)
    if (numWorkers <= 0 || numWorkers > 100) {
      throw ConfigurationException(s"Workers in platform service must be more than 0 and less than 100; value '$numWorkers' is invalid.")
    }
    numWorkers
  }

  lazy val persistDelay: Long = {
    val default = 10
    val period = readLong("persist-delay", default)
    logger.info("Platform service will persist snapshot changes every " + period + " seconds, and run with " + workers + " threads to push updates into the case system")
    period
  }
}