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

package com.casefabric.infrastructure.config.api

import com.typesafe.config.{Config, ConfigObject}
import com.casefabric.actormodel.identity.{CaseUserIdentity, Origin, PlatformUser, TenantUser}
import com.casefabric.cmmn.actorapi.command.team.{CaseTeam, CaseTeamUser}
import com.casefabric.cmmn.definition.CaseDefinition
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.infrastructure.config.util.{ChildConfigReader, ConfigReader}

class AnonymousConfig(val parent: ApiConfig) extends ChildConfigReader {
  lazy val anonymousUser = new AnonymousUserConfig(readConfig("user"))
  lazy val definitions: Map[String, AnonymousCaseDefinition] = {
    if (!config.hasPath("definitions")) {
      fail("CaseFabric anonymous API requires a set of definitions")
    }
    val definitionMap = scala.collection.mutable.Map[String, AnonymousCaseDefinition]()
    config.getObjectList("definitions").forEach(definitionConfig => {
      val definition = new AnonymousCaseDefinition(definitionConfig, anonymousUser)
      definitionMap.put(definition.url, definition).foreach(alreadyDefined => fail(s"The url '/request/case${definition.url}' --> '${} is already defined for case definition '${alreadyDefined.definition}'"))
    })
    definitionMap.toMap
  }
  def path = "anonymous-access"
  val enabled: Boolean = {
    val enabled = readBoolean("enabled", default = false)
    if (enabled) {
      requires("Anonymous access configuration", "user", "definitions")
    }
    enabled
  }
}

class AnonymousCaseDefinition(val myConfig: ConfigObject, val userConfig: AnonymousUserConfig) extends ConfigReader {
  lazy val definition: CaseDefinition = CaseFabric.config.repository.DefinitionProvider.read(anonymousPlatformUser, tenant, definitionFile).getFirstCase
  lazy val definitionFile: String = {
    val filename = readString("definition")
    if (filename.isBlank) {
      fail(s"Definition cannot be empty in anonymous case definition on url '/request/case/$url' --> ''")
    }
    filename
  }
  val config: Config = myConfig.toConfig
  requires("Case Definition for anonymous creation misses properties: ", "definition", "url", "caseTeam")
  // Validate url, definition and tenant contents; definition and tenant cannot be blank.
  val url: String = readString("url")
  val tenant: String = {
    val tenant = readString("tenant", CaseFabric.config.platform.defaultTenant)
    if (tenant.isBlank) {
      fail(s"Tenant is missing in anonymous case definition on url '/request/case/$url' --> '$definitionFile'; also default tenant is empty")
    }
    tenant
  }
  val team: CaseTeam = {
    val users = getConfigReader("caseTeam").readConfigList("users").map(reader => {
      reader.requires("Case Team user for definition " + definitionFile, "userId")
      new CaseTeamUser {
        override val userId: String = reader.readString("userId")
        override val origin: Origin = reader.readEnum("origin", classOf[Origin], Origin.Tenant)
        override val caseRoles: Set[String] = reader.readStringList("caseRoles").toSet
        override val isOwner: Boolean = reader.readBoolean("isOwner", default = false)
      }
    })

    val team = CaseTeam(users = users)
    // Check that there is at least one case owner defined
    if (team.owners.isEmpty) fail(s"Missing a case owner in the anonymous start case definition '/request/case/$url' --> '$definitionFile'")
    team
  }
  val anonymousPlatformUser: PlatformUser = userConfig.asUser(tenant)

  val user: CaseUserIdentity = CaseUserIdentity(userConfig.userId, Origin.Anonymous)

  override def toString: String = {
    def ownerMapper(m: CaseTeamUser): String = {
      if (m.isOwner) " as owner"
      else " as member"
    }

    s"StartCase '$definitionFile' with team [${team.users.map(m => s"${m.userId}'${ownerMapper(m)}").mkString(", ")}] "
  }
}

class AnonymousUserConfig(val config: Config) extends ConfigReader {
  requires("Anonymous user ", "id")
  val userId: String = readString("id")
  if (userId.isBlank) {
    fail("Anonymous user configuration must have a valid user id")
  }
  val roles: Set[String] = readStringList("roles").toSet
  val name: String = readString("name")
  val email: String = readString("email")

  def asUser(tenant: String): PlatformUser = PlatformUser(userId, Seq(TenantUser(id = userId, tenant = tenant, roles = roles, name = name, email = email)))
}
