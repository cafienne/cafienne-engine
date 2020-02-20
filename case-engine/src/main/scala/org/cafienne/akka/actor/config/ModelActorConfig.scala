package org.cafienne.akka.actor.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

class ModelActorConfig(val parentConfig: Config) extends LazyLogging {

  lazy val config : Config = {
    if (parentConfig.hasPath("actor")) {
      parentConfig.getConfig("actor")
    } else {
      null // yes, yes this shoudl be some option or so
    }
  }

  lazy val idlePeriod: Long = {
    if (config != null && config.hasPath("idle-period")) {
      val period = config.getLong("idle-period");
      logger.info("Individual Case instances will be removed from Akka memory after they have been idle for " + period + " seconds")
      period * 1000
    } else {
      val period = 60 * 10 // Default to 10 minutes.
      logger.info("Using default of 10 minutes to remove idle Case instances from Akka memory")
      period * 1000
    }
  }

  /**
    * Setting to indicate whether ModelActors should start in debug mode or not (by default).
    * Currently only implemented for cases. Also StartCase command has option to override the default setting
    */
  lazy val debugEnabled = {
    if (config != null && config.hasPath("debug")) {
      config.getBoolean("debug")
    } else if (parentConfig.hasPath("debug")) {
      parentConfig.getBoolean("debug")
    } else {
      false
    }
  }

}