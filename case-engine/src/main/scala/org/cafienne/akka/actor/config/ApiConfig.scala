package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.MandatoryConfig

class ApiConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "api"

  lazy val bindHost = {
    config.getString("bindhost")
  }

  lazy val bindPort = {
    config.getInt("bindport")
  }

  lazy val security: SecurityConfig = new SecurityConfig(this)
}
