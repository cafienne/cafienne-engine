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

  val anonymousConfig: AnonymousConfig = {
    new AnonymousConfig(this)
  }

  lazy val security: SecurityConfig = new SecurityConfig(this)
}
