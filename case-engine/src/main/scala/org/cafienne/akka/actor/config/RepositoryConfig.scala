package org.cafienne.akka.actor.config

import org.cafienne.cmmn.repository.DefinitionProvider

class RepositoryConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "definitions"
  override val exception = ConfigurationException("Cafienne Repository is not configured. Check local.conf for 'cafienne.definitions' settings")

  /**
    * DefinitionProvider provides an interface for loading Case Definitions
    */
  lazy val DefinitionProvider: DefinitionProvider = {
    val providerClassName = config.getString("provider")
    Class.forName(providerClassName).getDeclaredConstructor().newInstance().asInstanceOf[DefinitionProvider]
  }

  lazy val location: String = {
    config.getString("location")
  }

  lazy val cacheSize: Int = {
    if (config.hasPath("cache.size")) config.getInt("cache.size")
    100
  }
}