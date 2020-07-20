package org.cafienne.akka.actor.config

class OIDCConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "api.security.oidc"
  override val exception = ConfigurationException("Check configuration property 'cafienne.api.security.oidc'. This must be available.")

  val connectUrl = config.getString("connect-url")
  val tokenUrl = config.getString("token-url")
  val keysUrl = config.getString("key-url")
  val authorizationUrl = config.getString("authorization-url")
  val issuer = config.getString("issuer")
}