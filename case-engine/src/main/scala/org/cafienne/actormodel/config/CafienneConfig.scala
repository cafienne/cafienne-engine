package org.cafienne.actormodel.config

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.CafienneVersion
import org.cafienne.actormodel.config.util.ConfigReader
import org.cafienne.actormodel.identity.TenantUser

/**
  * Configuration settings of this Cafienne Case System Platform
  * @param systemConfig
  */
class CafienneConfig() extends ConfigReader with LazyLogging {
  val fallback = ConfigFactory.defaultReference()
  val systemConfig = ConfigFactory.load().withFallback(fallback)

  val path = "cafienne"
  override lazy val config = {
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
  lazy val readJournal = {
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
  lazy val OIDC: OIDCConfig = api.security.oidc

  /**
    * Returns configuration options for reading and writing case definitions
    */
  lazy val repository: RepositoryConfig = new RepositoryConfig(this)

  /**
    * Returns configuration options for the engine and it's internal services
    */
  lazy val engine: EngineConfig = new EngineConfig(this)

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

object Cafienne {

  /**
    * Configuration settings of this Cafienne Platform
    */
  lazy val config = new CafienneConfig

  /**
    * Returns the BuildInfo as a string (containing JSON)
    *
    * @return
    */
  lazy val version = new CafienneVersion

  def isPlatformOwner(user: TenantUser): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    config.platform.isPlatformOwner(userId)
  }
}