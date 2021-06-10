package org.cafienne.infrastructure.config

import org.cafienne.infrastructure.config.util.ChildConfigReader

class PlatformServiceConfig(val parent: EngineConfig) extends ChildConfigReader {
  val path = "platform-service"

  lazy val workers: Int = {
    val default = 5
    val numWorkers = readInt("workers", default)
    if (numWorkers <= 0 || numWorkers > 100) {
      fail(s"Workers in platform service must be more than 0 and less than 100; value '$numWorkers' is invalid.")
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