package org.cafienne.akka.actor.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.repository.DefinitionProvider

class ApiConfig(val parentConfig: Config) extends LazyLogging {

  lazy val config = {
    if (parentConfig.hasPath("api")) {
      parentConfig.getConfig("api")
    } else {
      throw new IllegalArgumentException("Cafienne API is not configured. Check local.conf for 'cafienne.api' settings")
    }
  }

  lazy val bindHost = {
    config.getString("bindhost")
  }

  lazy val bindPort = {
    config.getInt("bindport")
  }

}