package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.ChildConfigReader

class ModelActorConfig(val parent: CafienneConfig) extends ChildConfigReader {
  override def path = "actor"

  lazy val idlePeriod: Long = {
    val default = 60 * 10
    val period = readLong("idle-period", default)
    logger.info("Individual Case instances will be removed from Akka memory after they have been idle for " + period + " seconds")
    period * 1000
  }

  /**
    * Setting to indicate whether ModelActors should start in debug mode or not (by default).
    * Currently only implemented for cases. Also StartCase command has option to override the default setting
    */
  lazy val debugEnabled = {
    // Note: for now, we can better take this from model-actor config, but we should also read the old option
    readBoolean("debug", parent.readBoolean("debug", false))
  }
}