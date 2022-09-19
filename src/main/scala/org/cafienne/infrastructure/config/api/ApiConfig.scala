package org.cafienne.infrastructure.config.api

import org.cafienne.infrastructure.config.util.MandatoryConfig
import org.cafienne.infrastructure.config.CafienneConfig

class ApiConfig(val parent: CafienneConfig) extends MandatoryConfig {
  def path = "api"

  lazy val bindHost: String = {
    config.getString("bindhost")
  }

  lazy val bindPort: Int = {
    config.getInt("bindport")
  }

  val anonymousConfig: AnonymousConfig = {
    new AnonymousConfig(this)
  }

  lazy val security: SecurityConfig = new SecurityConfig(this)
}
