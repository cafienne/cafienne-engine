package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.MandatoryConfig

import java.util.Properties

class MailServiceConfig(val parent: EngineConfig) extends MandatoryConfig {
  override def path = "mail-service"

  lazy val asProperties: Properties = {
    val mailProperties = new Properties
    config.entrySet().forEach(entry => {
//      logger.warn(entry.getKey + ": " + entry.getValue.unwrapped)
      mailProperties.put(entry.getKey, entry.getValue.unwrapped)
    })
    mailProperties
  }
}