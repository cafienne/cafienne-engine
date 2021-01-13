package org.cafienne.akka.actor.config

import java.util.Properties

class MailServiceConfig(val parent: EngineConfig) extends MandatoryConfig {
  val path = "mail-service"
  override val exception = ConfigurationException("Check configuration property 'cafienne.engine.mail-service'. This must be available")

  lazy val asProperties: Properties = {
    val mailProperties = new Properties
    config.entrySet().forEach(entry => {
//      logger.warn(entry.getKey + ": " + entry.getValue.unwrapped)
      mailProperties.put(entry.getKey, entry.getValue.unwrapped)
    })
    mailProperties
  }
}