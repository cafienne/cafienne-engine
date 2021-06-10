package org.cafienne.infrastructure.config

import org.cafienne.infrastructure.config.util.MandatoryConfig

class SecurityConfig(val parent: ApiConfig) extends MandatoryConfig {
  val path = "security"

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