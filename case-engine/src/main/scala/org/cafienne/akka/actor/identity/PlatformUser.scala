package org.cafienne.akka.actor.identity

import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.exception.{AuthorizationException, MissingTenantException}
import org.cafienne.akka.actor.serialization.Fields
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.infrastructure.json.CafienneJson

final case class PlatformUser(userId: String, users: Seq[TenantUser]) extends CafienneJson {
  final def tenants: Seq[String] = users.map(u => u.tenant)
  final def tenants(tenant: Option[String]): Seq[String] = {
    tenant match {
      case Some(value) => Seq(value)
      case None => tenants
    }
  }

  /**
    * If the user is registered in one tenant only, that tenant is returned.
    * Otherwise, the default tenant of the platform is returned (even if the user is not part of it...???)
    * @return
    */
  final def defaultTenant: String = {
    if (tenants.length == 1) {
      tenants.head
    }
    val configuredDefaultTenant = CaseSystem.config.platform.defaultTenant
    if (configuredDefaultTenant.isEmpty) {
      throw new MissingTenantException("Tenant property must have a value")
    }
    configuredDefaultTenant
  }

  override def toValue  = {
    val map = new ValueMap(Fields.userId, userId)
    val userList = map.withArray("tenants")
    users.foreach(user => {
      userList.add(user.toValue)
    })
    map
  }

  final def shouldBelongTo(tenant: String) : Unit = users.find(u => u.tenant == tenant).getOrElse(throw AuthorizationException("Tenant '" + tenant +"' does not exist, or user '"+userId+"' is not registered in it"))

  final def isPlatformOwner: Boolean = CaseSystem.isPlatformOwner(userId)

  final def getTenantUser(tenant: String) = users.find(u => u.tenant == tenant).getOrElse({
    val message = tenants.isEmpty match {
      case true => s"User '$userId' is not registered in tenant '$tenant' (nor in any other tenant)"
      case false => "User '" + userId+"' is not registered in tenant '"+tenant+"'; tenants are: "+tenants.map(tenant => s"'$tenant'").mkString(",")
    }
    throw AuthorizationException(message)
  })
}