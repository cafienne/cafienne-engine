package org.cafienne.akka.actor.config

class ApiConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "api"
  override val exception = ConfigurationException("Cafienne API is not configured. Check local.conf for 'cafienne.api' settings")

  lazy val bindHost = {
    config.getString("bindhost")
  }

  lazy val bindPort = {
    config.getInt("bindport")
  }

}