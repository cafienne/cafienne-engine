package org.cafienne.infrastructure.config.api

import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata
import com.typesafe.config.{Config, ConfigValueType}
import org.cafienne.authentication.OIDCConfiguration
import org.cafienne.infrastructure.config.util.MandatoryConfig

import scala.jdk.CollectionConverters.ListHasAsScala

class OIDCConfig(val parent: SecurityConfig) extends MandatoryConfig {
  def path = "oidc"
  override lazy val config: Config = parent.config

  lazy val issuers: Seq[OIDCProviderMetadata] = {
    val list = if (config.hasPath(path)) {
      val configType = config.getValue(path).valueType()
      if (configType == ConfigValueType.LIST) {
        OIDCConfiguration.readConfigurations(config.getConfigList(path).asScala.toSeq)
      } else if (configType == ConfigValueType.OBJECT) {
        // This is the old style configuration on single IDP
        OIDCConfiguration.readConfigurations(Seq(config.getConfig(path)))
      } else {
        fail(s"Invalid type in $this configuration (found $configType, but expecting a LIST)")
      }
    } else {
      fail(s"Missing configuration property $this")
    }
    // Check that we have actual values in the list
    if (list.isEmpty) {
      fail(s"Configuration property $this cannot be left empty")
    }
    list
  }
}