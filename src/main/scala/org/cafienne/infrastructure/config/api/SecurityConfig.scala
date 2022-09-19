package org.cafienne.infrastructure.config.api

import com.typesafe.config.ConfigValueType
import org.cafienne.infrastructure.config.util.MandatoryConfig

import scala.jdk.CollectionConverters.ListHasAsScala

class SecurityConfig(val parent: ApiConfig) extends MandatoryConfig {
  def path = "security"

  lazy val oidcList: Seq[OIDCConfig] = {
    if (config.hasPath("multi-idp")) {
        val list = config.getConfigList("multi-idp").asScala.toSeq
        if (list.isEmpty) fail(s"Configuration property cafienne.$fullPath.multi-idp cannot be left empty")
        list.map(config => {
          new OIDCConfig(this, config)
        })
      } else if (config.hasPath(OIDCConfig.path)) {
      val configType = config.getValue(OIDCConfig.path).valueType()
      if (configType == ConfigValueType.LIST) {
        val list = config.getConfigList(OIDCConfig.path).asScala.toSeq
        if (list.isEmpty) fail(s"Configuration property cafienne.$fullPath.${OIDCConfig.path} cannot be left empty")
        list.map(config => {
          new OIDCConfig(this, config)
        })
      } else if (configType == ConfigValueType.OBJECT) {
        // This is the old style configuration on single IDP
        println("Running single style idp")
        Seq(new OIDCConfig(this))
      } else {
        fail(s"Invalid type in cafienne.$fullPath.${OIDCConfig.path} configuration (found $configType, but expecting a LIST)")
      }
    } else {
      fail(s"Missing configuration property cafienne.$fullPath.${OIDCConfig.path}")
    }
  }

  lazy val identityCacheSize: Int = {
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