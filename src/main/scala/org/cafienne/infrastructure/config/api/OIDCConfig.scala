package org.cafienne.infrastructure.config.api

import org.cafienne.infrastructure.config.util.MandatoryConfig

class OIDCConfig(val parent: SecurityConfig) extends MandatoryConfig {
  val path = "oidc"

  val connectUrl: String = config.getString("connect-url")
  val tokenUrl: String = config.getString("token-url")
  val keysUrl: String = config.getString("key-url")
  val authorizationUrl: String = config.getString("authorization-url")
  val issuer: String = config.getString("issuer")
}