package org.cafienne.akka.actor.config

import com.typesafe.config.{Config, ConfigObject}
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.config.util.{ChildConfigReader, ConfigReader}
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}
import org.cafienne.akka.actor.serialization.json.ValueMap
import org.cafienne.cmmn.akka.command.StartCase
import org.cafienne.cmmn.akka.command.team.{CaseTeam, CaseTeamMember, MemberKey}

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

  val tenant: String = readString("tenant", CaseSystem.config.platform.defaultTenant)
  val definition: String = readString("definition")
  val url: String = readString("url", definition)
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
    val definitionsDocument = CaseSystem.config.repository.DefinitionProvider.read(userConfig.asUser, tenant, definition)
    val sc = new StartCase(tenant, userConfig.asUser.getTenantUser(tenant), newCaseId, definitionsDocument.getFirstCase, inputParameters, team, debugMode)
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
  val asUser: PlatformUser = {
    val userId = readString("id")
    userId.isBlank match {
      case true => fail("Anonymous user configuration must have a valid user id")
      case false => {
        val tenant = readString("tenant", CaseSystem.config.platform.defaultTenant)
        tenant.isBlank match {
          case true => fail("Anonymous user configuration must have a tenant defined")
          case false =>
            val roles = readStringList("roles")
            val name = readString("name")
            val email = readString("email")
            PlatformUser(userId, Seq(TenantUser(userId, roles, tenant, false, name, email)))
        }
      }
    }
  }
}
