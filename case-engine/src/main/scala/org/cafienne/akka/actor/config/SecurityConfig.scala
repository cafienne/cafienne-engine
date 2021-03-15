package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.config.util.MandatoryConfig

class SecurityConfig(val parent: ApiConfig) extends MandatoryConfig {
  override def path = "security"

  lazy val oidc: OIDCConfig = new OIDCConfig(this)


  lazy val identityCacheSize = {
    val key = "identity.cache.size"
    val size = readInt(key, 1000)
    if (size == 0) {
      logger.info("Identity Caching is disabled")
    } else {
      logger.info("Running with Identity Cache of size " + size)
    }
    size
  }

}