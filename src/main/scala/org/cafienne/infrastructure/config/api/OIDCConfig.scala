package org.cafienne.infrastructure.config.api

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.util.ChildConfigReader

class OIDCConfig(val parent: SecurityConfig, val localConfig: Config = null) extends ChildConfigReader {
  override def config: Config = {
    if (localConfig == null) super.config
    else localConfig
  }
  override def path: String = OIDCConfig.path

  val connectUrl: String = readString("connect-url")
  val tokenUrl: String = readString("token-url")
  val keysUrl: String = readString("key-url")
  val authorizationUrl: String = readString("authorization-url")
  val issuer: String = readString("issuer")
}

object OIDCConfig {
  val path: String = "oidc"
}
