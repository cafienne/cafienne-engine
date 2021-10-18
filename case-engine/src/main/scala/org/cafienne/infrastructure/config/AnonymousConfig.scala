package org.cafienne.infrastructure.config

import com.typesafe.config.{Config, ConfigObject}
import org.cafienne.actormodel.identity.{PlatformUser, TenantUser}
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamMember, MemberKey}
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.config.util.{ChildConfigReader, ConfigReader}
import org.cafienne.json.ValueMap

class AnonymousConfig(val parent: ApiConfig) extends ChildConfigReader {
  val path = "anonymous-access"

  val enabled = readBoolean("enabled", false)
  if (enabled) {
    requires("Anonymous access configuration", "user", "definitions")
  }

  lazy val anonymousUser = new AnonymousUserConfig(readConfig("user"))

  lazy val definitions: Map[String, AnonymousCaseDefinition] = {
    if (! config.hasPath("definitions")) {
      fail("Cafienne anonymous API requires a set of definitions")
    }
    val definitionMap = scala.collection.mutable.Map[String, AnonymousCaseDefinition]()
    config.getObjectList("definitions").forEach(definitionConfig => {
      val definition = new AnonymousCaseDefinition(definitionConfig, anonymousUser)
      definitionMap.put(definition.url, definition).foreach(alreadyDefined => fail(s"The url '/request/case${definition.url}' --> '${} is already defined for case definition '${alreadyDefined.definition}'"))
    })
    definitionMap.toMap
  }
}

class AnonymousCaseDefinition(val myConfig: ConfigObject, val userConfig: AnonymousUserConfig) extends ConfigReader {
  val config: Config = myConfig.toConfig
  requires("Case Definition for anonymous creation misses properties: ", "definition", "url", "caseTeam")

  // Validate url, definition and tenant contents; definition and tenant cannot be blank.
  val url: String = readString("url")
  val definition: String = readString("definition")
  if (definition.isBlank) {
    fail(s"Definition cannot be empty in anonymous case definition on url '/request/case/$url' --> '$definition'")
  }
  val tenant: String = readString("tenant", Cafienne.config.platform.defaultTenant)
  if (tenant.isBlank) {
    fail(s"Tenant is missing in anonymous case definition on url '/request/case/$url' --> '$definition'; also default tenant is empty")
  }
  val anonymousPlatformUser = userConfig.asUser(tenant)
  val anonymousTenantUser = anonymousPlatformUser.getTenantUser(tenant)
  val team: CaseTeam = {
    val members: Seq[CaseTeamMember] = getConfigReader("caseTeam").readConfigList("members").map(reader => {
      reader.requires("Case Team member for definition " + definition, "memberId", "memberType")
      val memberId = reader.readString("memberId")
      val memberType = reader.readString("memberType")
      val caseRoles = reader.readStringList("caseRoles")
      val isOwner = reader.readBoolean("isOwner", false)
      new CaseTeamMember(MemberKey(memberId, memberType), caseRoles, Some(isOwner))
    })

    // Check that there is at least one case owner defined
    members.find(_.isOwner.getOrElse(false)).getOrElse(fail(s"Missing a case owner in the anonymous start case definition '/request/case/$url' --> '$definition'"))

    CaseTeam(members)
  }

  def createStartCaseCommand(newCaseId: String, inputParameters: ValueMap, debugMode: Boolean) = {
    val definitionsDocument = Cafienne.config.repository.DefinitionProvider.read(anonymousPlatformUser, tenant, definition)
    val sc = new StartCase(tenant, anonymousTenantUser, newCaseId, definitionsDocument.getFirstCase, inputParameters, team, debugMode)
    sc
  }

  override def toString: String = {
    def ownerMapper(m: CaseTeamMember) = {
      m.isOwner.getOrElse(false) match {
        case true => " as owner"
        case false => " as member"
      }
    }
    s"StartCase '$definition' with team [${team.members.map(m => s"${m.key.`type`} '${m.key.id}'${ownerMapper(m)}" ).mkString(", ")}] "
  }
}

class AnonymousUserConfig(val config: Config) extends ConfigReader {
  requires("Anonymous user ", "id")
  val userId = readString("id")
  if (userId.isBlank) {
    fail("Anonymous user configuration must have a valid user id")
  }
  val roles = readStringList("roles").toSet
  val name = readString("name")
  val email = readString("email")

  def asUser(tenant: String): PlatformUser = PlatformUser(userId, Seq(TenantUser(userId, roles, tenant, false, name, email)))
}
