package org.cafienne.akka.actor.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.repository.DefinitionProvider

class RepositoryConfig(val parentConfig: Config) extends LazyLogging {

  lazy val config = {
    if (parentConfig.hasPath("definitions")) {
      parentConfig.getConfig("definitions")
    } else {
      throw new IllegalArgumentException("Cafienne Repository is not configured. Check local.conf for 'cafienne.definitions' settings")
    }
  }

  /**
    * DefinitionProvider provides an interface for loading Case Definitions
    */
  lazy val DefinitionProvider: DefinitionProvider = {
    val providerClassName = config.getString("provider")
    Class.forName(providerClassName).newInstance().asInstanceOf[DefinitionProvider]
  }

  lazy val location: String = {
    config.getString("location")
  }
}