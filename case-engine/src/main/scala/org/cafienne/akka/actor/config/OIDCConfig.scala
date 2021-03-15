package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.MandatoryConfig

class OIDCConfig(val parent: SecurityConfig) extends MandatoryConfig {
  override def path = "oidc"

  val connectUrl = config.getString("connect-url")
  val tokenUrl = config.getString("token-url")
  val keysUrl = config.getString("key-url")
  val authorizationUrl = config.getString("authorization-url")
  val issuer = config.getString("issuer")
}