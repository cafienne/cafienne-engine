package org.cafienne.akka.actor.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

/**
  * Configuration settings of this Cafienne Case System Platform
  * @param systemConfig
  */
class CafienneConfig(val systemConfig: Config) extends LazyLogging {

  val config = {
    if (systemConfig.hasPath("cafienne")) {
      systemConfig.getConfig("cafienne")
    } else {
      throw new IllegalArgumentException("Cafienne System is not configured. Check local.conf for 'cafienne' settings")
    }
  }

  /**
    * Returns configuration options for the platform, e.g. default tenant, list of platform owners
    */
  val platform: PlatformConfig = {
    new PlatformConfig(config)
  }

  /**
    * Returns the Open ID Connect configuration settings of this Case System
    */
  lazy val OIDC: OIDCConfig = {
    new OIDCConfig(config)
  }

  /**
    * Returns configuration options for the QueryDB
    */
  lazy val queryDB: QueryDBConfig = {
    new QueryDBConfig(config)
  }

  /**
    * Returns configuration options for Model Actors
    */
  lazy val actor: ModelActorConfig = {
    new ModelActorConfig(config)
  }

  /**
    * Returns configuration options for the HTTP APIs
    */
  lazy val api: ApiConfig = {
    new ApiConfig(config)
  }

  /**
    * Returns configuration options for reading and writing case definitions
    */
  lazy val repository: RepositoryConfig = {
    new RepositoryConfig(config)
  }

  /**
    * Returns true of the debug route is open (for developers using IDE to do debugging)
    */
  val developerRouteOpen = {
    val debugRouteOpenOption = "api.security.debug.events.open"
    if (config.hasPath(debugRouteOpenOption)) {
      val open = config.getBoolean(debugRouteOpenOption)
      if (open) {
        val manyHashes = "\n\n############################################################################################################\n\n"
        logger.warn(manyHashes+"\tWARNING - Case Service runs in developer mode (the debug route to get all events is open for anyone!)" + manyHashes)
      }
      open
    } else {
      false
    }
  }

}
