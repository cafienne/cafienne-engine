package org.cafienne.akka.actor.config

import com.typesafe.config.Config

/**
  * Configuration settings of this Cafienne Case System Platform
  * @param systemConfig
  */
class CafienneConfig(val systemConfig: Config) extends CafienneBaseConfig {
  val parent = null
  val path = "cafienne"

  override lazy val config = {
    if (systemConfig.hasPath(path)) {
      systemConfig.getConfig(path)
    } else {
      throw new IllegalArgumentException("Cafienne System is not configured. Check local.conf for 'cafienne' settings")
    }
  }

  /**
    * Returns configuration options for the platform, e.g. default tenant, list of platform owners
    */
  val platform: PlatformConfig = new PlatformConfig(this)

  /**
    * Returns the Open ID Connect configuration settings of this Case System
    */
  lazy val OIDC: OIDCConfig = new OIDCConfig(this)

  /**
    * Returns configuration options for the QueryDB
    */
  lazy val queryDB: QueryDBConfig = new QueryDBConfig(this)

  /**
    * Returns configuration options for Model Actors
    */
  lazy val actor: ModelActorConfig = new ModelActorConfig(this)

  /**
    * Returns configuration options for the HTTP APIs
    */
  lazy val api: ApiConfig = new ApiConfig(this)

  /**
    * Returns configuration options for reading and writing case definitions
    */
  lazy val repository: RepositoryConfig = new RepositoryConfig(this)

  /**
    * Returns true of the debug route is open (for developers using IDE to do debugging)
    */
  val developerRouteOpen = {
    val debugRouteOpenOption = "api.security.debug.events.open"
    val open = readBoolean(debugRouteOpenOption, false)
    if (open) {
      val manyHashes = "\n\n############################################################################################################\n\n"
      logger.warn(manyHashes+"\tWARNING - Case Service runs in developer mode (the debug route to get all events is open for anyone!)" + manyHashes)
    }
    open
  }
}

