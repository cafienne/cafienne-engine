package org.cafienne.akka.actor.identity

import org.cafienne.akka.actor.CaseSystem
import org.cafienne.cmmn.instance.casefile.ValueMap

final case class PlatformUser(userId: String, users: Seq[TenantUser]) {
  final def tenants: Seq[String] = users.map(u => u.tenant)
  final def tenants(tenant: Option[String]): Seq[String] = {
    tenant match {
      case Some(value) => Seq(value)
      case None => tenants
    }
  }

  final def toJSON: String  = {
    val map = new ValueMap(Fields.userId, userId)
    val userList = map.withArray("tenants")
    users.foreach(user => {
      userList.add(user.toJson)
    })
    map.toString
  }

  final def shouldBelongTo(tenant: String) : Unit = users.find(u => u.tenant == tenant).getOrElse(throw new SecurityException("Tenant " + tenant +" does not exist, or user is not registered in it"))

  final def isPlatformOwner: Boolean = CaseSystem.isPlatformOwner(userId)

  final def getTenantUser(tenant: String) = users.find(u => u.tenant == tenant).getOrElse(throw new SecurityException("User '" + userId+"' is not registered in tenant "+tenant+"; tenants are: "+tenants))
}