package org.cafienne.infrastructure.config

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.api.{ApiConfig, OIDCConfig}
import org.cafienne.infrastructure.config.engine.EngineConfig
import org.cafienne.infrastructure.config.util.{ConfigReader, SystemConfig}

/**
  * Configuration settings of this Cafienne Case System Platform
  * @param systemConfig
  */
class CafienneConfig extends ConfigReader with LazyLogging {
  val systemConfig: Config = SystemConfig.load()

  val path = "cafienne"
  override lazy val config: Config = {
    if (systemConfig.hasPath(path)) {
      systemConfig.getConfig(path)
    } else {
      fail("Cafienne System is not configured. Check local.conf for 'cafienne' settings")
    }
  }

  /**
    * Returns configuration options for the platform, e.g. default tenant, list of platform owners
    */
  val platform: PlatformConfig = new PlatformConfig(this)

  /**
    * Returns configuration options for the QueryDB
    */
  lazy val readJournal: String = {
    if (config.hasPath("read-journal")) {
      readString("read-journal")
    } else {
      queryDB.readJournal
    }
  }

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
    * Returns the Open ID Connect configuration settings of this Case System
    */
  //TODO change the config to support more than one OIDCConfig to enable multi-issuer
  //NOTE that we should configure the issuer URL and use that for validation, for security reasons ????
  lazy val OIDC: OIDCConfig = api.security.oidc

  /**
    * Returns configuration options for reading and writing case definitions
    */
  lazy val repository: RepositoryConfig = new RepositoryConfig(this)

  /**
    * Returns configuration options for the engine and it's internal services
    */
  val engine: EngineConfig = new EngineConfig(this)

  /**
    * Returns true of the debug route is open (for developers using IDE to do debugging)
    */
  val developerRouteOpen: Boolean = {
    val debugRouteOpenOption = "api.security.debug.events.open"
    val open = readBoolean(debugRouteOpenOption, default = false)
    if (open) {
      SystemConfig.printWarning("Case Service runs in developer mode (the debug route to get all events is open for anyone!)")
    }
    open
  }
}

