package org.cafienne.infrastructure.config

import org.cafienne.infrastructure.config.util.MandatoryConfig

import java.util.Properties

class MailServiceConfig(val parent: EngineConfig) extends MandatoryConfig {
  val path = "mail-service"

  lazy val asProperties: Properties = {
    val mailProperties = new Properties
    config.entrySet().forEach(entry => {
//      logger.warn(entry.getKey + ": " + entry.getValue.unwrapped)
      mailProperties.put(entry.getKey, entry.getValue.unwrapped)
    })
    mailProperties
  }
}