package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.ChildConfigReader

class EngineConfig(val parent: CafienneConfig) extends ChildConfigReader {
  override def path = "engine"

  /**
    * Returns configuration options for the Platform Service
    */
  lazy val platformServiceConfig = new PlatformServiceConfig(this)

  /**
    * Returns configuration options for the Timer Service
    */
  lazy val timerService = new TimerServiceConfig(this)

  /**
    * Config property for settings of the mail service to use
    */
  lazy val mailService = new MailServiceConfig(this)

  /**
    * Returns configuration options for uploading and downloading case file documents
    */
  lazy val documentService = new DocumentServiceConfig(this)
}
