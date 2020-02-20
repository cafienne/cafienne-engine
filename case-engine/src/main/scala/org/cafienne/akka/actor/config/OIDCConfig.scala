package org.cafienne.akka.actor.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

class OIDCConfig(val parentConfig: Config) extends LazyLogging {

  val config = {
    if (parentConfig.hasPath("api.security.oidc")) {
      parentConfig.getConfig("api.security.oidc")
    } else {
      throw new IllegalArgumentException("Check configuration property 'cafienne.api.security.oidc'. This must be available.")
    }
  }

  val connectUrl = config.getString("connect-url")
  val tokenUrl = config.getString("token-url")
  val keysUrl = config.getString("key-url")
  val authorizationUrl = config.getString("authorization-url")
  val issuer = config.getString("issuer")
}