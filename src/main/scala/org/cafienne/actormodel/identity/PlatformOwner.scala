package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.ValueMap

case class PlatformOwner(id: String) extends UserIdentity {
  def asTenantUser(tenant: String): TenantUser = TenantUser(id = id, tenant = tenant)

  override def asCaseUserIdentity(): CaseUserIdentity = CaseUserIdentity(id, Origin.PlatformOwner)
}

object PlatformOwner {
  def deserialize(json: ValueMap): PlatformOwner = {
    val userId: String = json.readString(Fields.userId)
    PlatformOwner(userId)
  }
}
