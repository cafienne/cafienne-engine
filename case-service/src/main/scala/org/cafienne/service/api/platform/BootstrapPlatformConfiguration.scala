package org.cafienne.service.api.platform

import java.io.File

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.response.{CommandFailure, ModelResponse}
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.service.Main
import org.cafienne.tenant.akka.command.platform.BootstrapTenant
import org.cafienne.tenant.akka.command.response.TenantResponse

import scala.collection.JavaConverters._

/**
  * The platform can be configured with a default tenant setup.
  * Through this class, that setup is parsed and registered in the case system
  * Setup files can be of type json or yaml. Setup file must exist as a concatenation of the configured default tenant
  * name along with .json, .yaml or .yml
  */
object BootstrapPlatformConfiguration extends LazyLogging {
  def run(): Unit = {
    try {
      findConfigFile.map{parseConfigFile}.map{sendCommand}
    } catch {
      case b: BootstrapFailure => throw b
      case t: Throwable => throw new BootstrapFailure("Unexpected error while reading bootstrap configuration", t)
    }
  }

  private def findConfigFile(): Option[File] = {
    logger.debug("Checking for any bootstrap configuration of the case system")
    val defaultTenant = CaseSystem.config.platform.defaultTenant
    if (defaultTenant.trim.isEmpty) {
      logger.info("Default tenant is empty. Skipping bootstrap attempts")
      return None
    }

    val jsonFile = new File(defaultTenant + ".json")
    if (jsonFile.exists()) return Some(jsonFile)
    val ymlFile = new File(defaultTenant + ".yml")
    if (ymlFile.exists()) return Some(ymlFile)
    val yamlFile = new File(defaultTenant + ".yaml")
    if (yamlFile.exists()) return Some(yamlFile)

    logger.info(s"Skipping bootstrap tenant configuration for '$defaultTenant', because a file '${jsonFile}', '${ymlFile}' or '${yamlFile}' cannot be found")
    None
   }

  private def parseConfigFile(configFile: File): BootstrapTenant = {
    val defaultTenant = CaseSystem.config.platform.defaultTenant
    logger.info(s"Bootstrapping tenant '$defaultTenant' from file ${configFile.getAbsolutePath}")

    val tenantConfig: Config = ConfigFactory.parseFile(configFile)

    try {
      val tenantName: String = tenantConfig.getString("name")
      if (! tenantConfig.hasPath("owners")) {
        throw new BootstrapFailure("Bootstrap file should contain a list of owners, with at least one owner for the tenant")
      }
      val ownerIds = tenantConfig.getStringList("owners").asScala // Owners MUST exist
      if (ownerIds.isEmpty) {
        throw new BootstrapFailure("Bootstrap file should contain a list of owners, with at least one owner for the tenant")
      }

      val users: Seq[TenantUser] = tenantConfig.getConfigList("users").asScala.map(user => {
        val userId = user.getString("id")
        val roles = readStringList(user, "roles")
        val userName = readStringOr(user, "name", "")
        val email = readStringOr(user, "email", "")
        new TenantUser(userId, roles, tenantName, userName, email, true)
      })

      val undefinedOwners = ownerIds.filter(id => !users.map(u => u.id).contains(id))
      if (!undefinedOwners.isEmpty) {
        throw new BootstrapFailure("All bootstrap tenant owners must be defined as user. Following users not found: " + undefinedOwners)
      }

      val owners = users.filter(user => ownerIds.contains(user.id)).toSet.asJava
      val plainTenantUsers = users.filter(user => !ownerIds.contains(user.id)).toSet.asJava

      val aPlatformOwner = new PlatformUser(CaseSystem.config.platform.platformOwners.get(0), Seq())

      new BootstrapTenant(aPlatformOwner, tenantName, tenantName, owners, plainTenantUsers)

    } catch {
      case c: ConfigException => throw new BootstrapFailure("Bootstrap file " + configFile.getAbsolutePath+" is invalid: " + c.getMessage, c)
    }
  }

  private def readStringOr(config: Config, path: String, defaultValue: String): String = {
    if (config.hasPath(path)) {
      config.getString(path)
    } else {
      defaultValue
    }
  }

  private def readStringList(config: Config, path: String, defaultValue: Seq[String] = Seq()): Seq[String] = {
    if (config.hasPath(path)) {
      config.getStringList(path).asScala
    } else {
      defaultValue
    }
  }

  private def sendCommand(bootstrapTenant: BootstrapTenant) = {
    import akka.pattern.ask
    implicit val timeout = Main.caseSystemTimeout
    implicit val ec = scala.concurrent.ExecutionContext.global

    CaseSystem.router.ask(bootstrapTenant).map(response =>
      response match {
        case e: CommandFailure => {
          if (e.exception().getMessage.toLowerCase().contains("already exists")) {
            logger.info("Bootstrap tenant '" + bootstrapTenant.name + "' already exists; ignoring bootstrap info")
          } else {
            logger.warn("Bootstrap tenant '" + bootstrapTenant.name + "' creation failed with an unexpected exception", e)
          }
        }
        case t: TenantResponse => logger.info("Completed creation of bootstrap tenant '" + bootstrapTenant.name + "'")
        case r: ModelResponse => logger.info("Unexpected response during creation of bootstrap tenant: " + r)
        case t: Throwable => throw t
        case other => logger.error("Unexpected response during creation of bootstrap tenant, of type " + other.getClass.getName)
      }
    )
  }
}

class BootstrapFailure(msg: String, t: Throwable = new Exception()) extends Exception(msg, t)