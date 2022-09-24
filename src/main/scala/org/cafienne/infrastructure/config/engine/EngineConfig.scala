package org.cafienne.infrastructure.config.engine

import org.cafienne.infrastructure.config.util.ChildConfigReader
import org.cafienne.infrastructure.config.CafienneConfig

class EngineConfig(val parent: CafienneConfig) extends ChildConfigReader {
  def path = "engine"

  /**
    * Returns configuration options for the Timer Service
    */
  val timerService = new TimerServiceConfig(this)

  /**
    * Config property for settings of the mail service to use
    */
  lazy val mailService = new MailServiceConfig(this)
}
