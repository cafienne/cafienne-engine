/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.system.bootstrap

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.actormodel.response.{ActorExistsFailure, CommandFailure, ModelResponse}
import org.cafienne.system.CaseSystem
import org.cafienne.userregistration.tenant.actorapi.command.platform.CreateTenant
import org.cafienne.userregistration.tenant.actorapi.response.TenantResponse

import java.io.File
import scala.concurrent._
import scala.jdk.CollectionConverters._

/**
  * The platform can be configured with a default tenant setup.
  * Through this class, that setup is parsed and registered in the case system
  * Setup files can be of type json or yaml. Setup file must exist as a concatenation of the configured default tenant
  * name along with .json, .yaml or .yml
  */
object BootstrapPlatformConfiguration extends LazyLogging {

  def run(implicit caseSystem: CaseSystem): Unit = {
    try {
      findConfigFile.map(parseConfigFile).map(c => sendCommand(caseSystem, c))
    } catch {
      case b: BootstrapFailure => throw b
      case t: Throwable => throw new BootstrapFailure("Unexpected error while reading bootstrap configuration", t)
    }
  }

  private def findConfigFile(implicit caseSystem: CaseSystem): Seq[File] = {
    logger.info("Checking presence of bootstrap configuration for the case system")
    val bootstrapTenantConfFiles = caseSystem.config.platform.bootstrapFile
    if (bootstrapTenantConfFiles.nonEmpty) {
      val files = bootstrapTenantConfFiles.map(fileName => {
        val configFile = new File(fileName)
        if (!configFile.exists()) {
          logger.warn("Sleeping a bit, because file " + fileName + " seems to not (yet) exist")
          Thread.sleep(1000) // Sometimes in docker, volume is not mounted fast enough it seems. Therefore we put a wait statement of 1 second and then check again.
          if (!configFile.exists()) {
            throw new BootstrapFailure(s"The configured bootstrap tenant file cannot be found at '${configFile.getAbsolutePath}' (conf value: '$fileName')")
          }
          logger.warn("Sleeping a bit helped, because file " + fileName + " now exists")
        }
        configFile
      })
      return files
    }

    val defaultTenant = caseSystem.config.platform.defaultTenant
    if (defaultTenant.isBlank) {
      logger.warn("Default tenant is empty and bootstrap-file is not filled. Skipping bootstrap attempts")
      return Seq()
    }

    val confFile = new File(defaultTenant + ".conf")
    if (confFile.exists()) return Seq(confFile)
    val jsonFile = new File(defaultTenant + ".json")
    if (jsonFile.exists()) return Seq(jsonFile)
    val ymlFile = new File(defaultTenant + ".yml")
    if (ymlFile.exists()) return Seq(ymlFile)
    val yamlFile = new File(defaultTenant + ".yaml")
    if (yamlFile.exists()) return Seq(yamlFile)

    logger.warn(s"Skipping bootstrap tenant configuration for '$defaultTenant', because a file '$confFile', '$jsonFile', '$ymlFile' or '$yamlFile' cannot be found")
    Seq()
  }

  private def parseConfigFile(configFile: File)(implicit caseSystem: CaseSystem): CreateTenant = {
    val defaultTenant = caseSystem.config.platform.defaultTenant
    logger.info(s"Bootstrapping tenant '$defaultTenant' from file ${configFile.getAbsolutePath}")

    val tenantConfig: Config = ConfigFactory.parseFile(configFile)

    try {
      val tenantName: String = tenantConfig.getString("name")

      val ownerIds = {
        if (tenantConfig.hasPath("owners")) {
          val list = tenantConfig.getStringList("owners").asScala
          logger.warn(s"""Bootstrap tenant '$tenantName' in file '${configFile.getName}' uses deprecated property 'owners = [${list.mkString("\"", "\", \"", "\"")}]'. Use 'isOwner = true' inside the designated users instead.""")
          list
        } else {
          Seq()
        }
      } // Owners MUST exist

      val users: Seq[TenantUser] = tenantConfig.getConfigList("users").asScala.toSeq.map(user => {
        val userId = user.getString("id")
        val roles = readStringList(user, "roles").toSet
        val name = readStringOr(user, "name", "")
        val email = readStringOr(user, "email", "")
        val isOwner = readBooleanOr(user, "isOwner", ownerIds.contains(userId))
        TenantUser(id = userId, tenant = tenantName, roles = roles, name = name, email = email, isOwner = isOwner)
      })

      if (!users.exists(_.isOwner)) {
        throw new BootstrapFailure(s"Bootstrap tenant '$tenantName' misses a mandatory tenant owner. File ${configFile.getAbsolutePath}")
      }

      val undefinedOwners = ownerIds.filter(id => !users.map(u => u.id).contains(id))
      if (undefinedOwners.nonEmpty) {
        val msg = s"""Bootstrap tenant '$tenantName' in file ${configFile.getAbsolutePath} mentions owner(s) [${undefinedOwners.mkString("\"", "\", \"", "\"")}], but corresponding users are not defined in the file."""
        logger.error("FATAL ERROR: " + msg)
        throw new BootstrapFailure(msg)
      }

      val aPlatformOwner = caseSystem.config.platform.platformOwners.head

      new CreateTenant(aPlatformOwner, tenantName, tenantName, users.asJava)

    } catch {
      case c: ConfigException => throw new BootstrapFailure("Bootstrap file " + configFile.getAbsolutePath + " is invalid: " + c.getMessage, c)
    }
  }

  private def readBooleanOr(config: Config, path: String, defaultValue: Boolean): Boolean = {
    if (config.hasPath(path)) {
      config.getBoolean(path)
    } else {
      defaultValue
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
      config.getStringList(path).asScala.toSeq
    } else {
      defaultValue
    }
  }

  private def sendCommand(caseSystem: CaseSystem, bootstrapTenant: CreateTenant): Future[Unit] = {
    implicit val ec: ExecutionContext = caseSystem.system.dispatcher

    caseSystem.engine.request(bootstrapTenant).map {
      case _: ActorExistsFailure => logger.warn(s"Bootstrap tenant '${bootstrapTenant.name}' already exists; ignoring bootstrap info (even if it is updated)")
      case e: CommandFailure => logger.warn(s"Bootstrap tenant '${bootstrapTenant.name}' creation failed with an unexpected exception", e)
      case _: TenantResponse => logger.warn(s"Completed creation of bootstrap tenant '${bootstrapTenant.name}'")
      case r: ModelResponse => logger.info("Unexpected response during creation of bootstrap tenant: " + r)
      case other => logger.error("Unexpected response during creation of bootstrap tenant, of type " + other.getClass.getName)
    }
  }
}

class BootstrapFailure(msg: String, t: Throwable = new Exception()) extends Exception(msg, t)