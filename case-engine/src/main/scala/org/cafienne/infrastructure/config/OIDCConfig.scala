package org.cafienne.infrastructure.config

import org.cafienne.infrastructure.config.util.MandatoryConfig

class OIDCConfig(val parent: SecurityConfig) extends MandatoryConfig {
  val path = "oidc"

  val connectUrl = config.getString("connect-url")
  val tokenUrl = config.getString("token-url")
  val keysUrl = config.getString("key-url")
  val authorizationUrl = config.getString("authorization-url")
  val issuer = config.getString("issuer")
}