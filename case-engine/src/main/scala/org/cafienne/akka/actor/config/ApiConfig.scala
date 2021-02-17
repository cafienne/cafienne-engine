package org.cafienne.akka.actor.config

import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.identity.{PlatformUser, TenantUser}

class ApiConfig(val parent: CafienneConfig) extends MandatoryConfig {
  val path = "api"
  override val exception = ConfigurationException("Cafienne API is not configured. Check local.conf for 'cafienne.api' settings")

  lazy val bindHost = {
    config.getString("bindhost")
  }

  lazy val bindPort = {
    config.getInt("bindport")
  }

  val anonymousUser: Option[PlatformUser] = {
    new AnonymousUserConfig(this).asUser
  }

  lazy val security: SecurityConfig = new SecurityConfig(this)
}

class AnonymousUserConfig(val parent: ApiConfig) extends CafienneBaseConfig {
  val path = "anonymous-user"
  val asUser: Option[PlatformUser] = {
    val userId = readString("id", "")
    userId.isBlank match {
      case true => None
      case false => {
        val tenant = readString("tenant", CaseSystem.config.platform.defaultTenant)
        tenant.isBlank match {
          case true => throw ConfigurationException("Anonymous user configuration must have a tenant defined")
          case false =>
            val roles = readStringList("roles")
            val name = readString("name", "")
            val email = readString("email", "")
            Some(PlatformUser(userId, Seq(TenantUser(userId, roles, tenant, false, name, email, enabled = true, isAnonymous = true))))
        }
      }
    }
  }
}