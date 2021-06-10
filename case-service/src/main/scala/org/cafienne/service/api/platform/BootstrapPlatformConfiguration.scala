package org.cafienne.service.api.platform

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.response.{CommandFailure, ModelResponse}
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.actormodel.command.response.CommandFailure
import org.cafienne.infrastructure.Cafienne
import org.cafienne.service.Main
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command.TenantUserInformation
import org.cafienne.tenant.actorapi.command.platform.CreateTenant
import org.cafienne.tenant.actorapi.response.TenantResponse

import java.io.File
import scala.collection.JavaConverters._

/**
  * The platform can be configured with a default tenant setup.
  * Through this class, that setup is parsed and registered in the case system
  * Setup files can be of type json or yaml. Setup file must exist as a concatenation of the configured default tenant
  * name along with .json, .yaml or .yml
  */
object BootstrapPlatformConfiguration extends LazyLogging {

  def run(caseSystem: CaseSystem): Unit = {
    try {
      findConfigFile.map{parseConfigFile}.map{c => sendCommand(caseSystem, c)}
    } catch {
      case b: BootstrapFailure => throw b
      case t: Throwable => throw new BootstrapFailure("Unexpected error while reading bootstrap configuration", t)
    }
  }

  private def findConfigFile(): Option[File] = {
    logger.warn("Checking presence of bootstrap configuration for the case system")
    val bootstrapTenantConfFileName = Cafienne.config.platform.bootstrapFile
    if (!bootstrapTenantConfFileName.isBlank) {
      val configFile = new File(bootstrapTenantConfFileName)
      if (! configFile.exists()) {
        logger.warn("Sleeping a bit, becuase file " + bootstrapTenantConfFileName+" seems to not (yet) exist")
        Thread.sleep(1000) // Sometimes in docker, volume is not mounted fast enough it seems. Therefore we put a wait statement of 1 second and then check again.
        if (! configFile.exists()) {
          throw new BootstrapFailure(s"The configured bootstrap tenant file cannot be found at '${configFile.getAbsolutePath}' (conf value: '$bootstrapTenantConfFileName')")
        }
        logger.warn("Sleeping a bit helped, becuase file " + bootstrapTenantConfFileName+" now exists")
      }
      return Some(configFile)
    }

    val defaultTenant = Cafienne.config.platform.defaultTenant
    if (defaultTenant.isBlank) {
      logger.warn("Default tenant is empty and bootstrap-file is not filled. Skipping bootstrap attempts")
      return None
    }

    val confFile = new File(defaultTenant + ".conf")
    if (confFile.exists()) return Some(confFile)
    val jsonFile = new File(defaultTenant + ".json")
    if (jsonFile.exists()) return Some(jsonFile)
    val ymlFile = new File(defaultTenant + ".yml")
    if (ymlFile.exists()) return Some(ymlFile)
    val yamlFile = new File(defaultTenant + ".yaml")
    if (yamlFile.exists()) return Some(yamlFile)

    logger.warn(s"Skipping bootstrap tenant configuration for '$defaultTenant', because a file '$confFile', '$jsonFile', '$ymlFile' or '$yamlFile' cannot be found")
    None
   }

  private def parseConfigFile(configFile: File): CreateTenant = {
    val defaultTenant = Cafienne.config.platform.defaultTenant
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

      val users: Seq[TenantUserInformation] = tenantConfig.getConfigList("users").asScala.map(user => {
        val userId = user.getString("id")
        val roles = readStringList(user, "roles")
        val name = readStringOr(user, "name", "")
        val email = readStringOr(user, "email", "")
        val isOwner = ownerIds.contains(userId)
        TenantUserInformation(userId, roles = Some(roles), name = Some(name), email = Some(email), owner = Some(isOwner), enabled = Some(true))
      })

      val undefinedOwners = ownerIds.filter(id => !users.map(u => u.id).contains(id))
      if (!undefinedOwners.isEmpty) {
        throw new BootstrapFailure("All bootstrap tenant owners must be defined as user. Following users not found: " + undefinedOwners)
      }

      val aPlatformOwner = PlatformUser(Cafienne.config.platform.platformOwners.get(0), Seq())

      new CreateTenant(aPlatformOwner, tenantName, tenantName, users.asJava)

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

  private def sendCommand(caseSystem: CaseSystem, bootstrapTenant: CreateTenant) = {
    import akka.pattern.ask
    implicit val timeout = Main.caseSystemTimeout
    implicit val ec = scala.concurrent.ExecutionContext.global

    caseSystem.router.ask(bootstrapTenant).map(response =>
      response match {
        case e: CommandFailure => {
          if (e.exception().getMessage.toLowerCase().contains("already exists")) {
            logger.info(s"Bootstrap tenant '${bootstrapTenant.name}' already exists; ignoring bootstrap info")
          } else {
            logger.warn(s"Bootstrap tenant '${bootstrapTenant.name}' creation failed with an unexpected exception", e)
          }
        }
        case t: TenantResponse => logger.warn(s"Completed creation of bootstrap tenant '${bootstrapTenant.name}'")
        case r: ModelResponse => logger.info("Unexpected response during creation of bootstrap tenant: " + r)
        case t: Throwable => throw t
        case other => logger.error("Unexpected response during creation of bootstrap tenant, of type " + other.getClass.getName)
      }
    )
  }
}

class BootstrapFailure(msg: String, t: Throwable = new Exception()) extends Exception(msg, t)