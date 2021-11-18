package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.ValueMap

case class PlatformOwner(id: String) extends UserIdentity {
  def asTenantUser(tenant: String) = TenantUser(id = id, roles = Set(), tenant = tenant, name = "")
}

object PlatformOwner {
  def deserialize(json: ValueMap): PlatformOwner = {
    val userId: String = json.readString(Fields.userId)
    PlatformOwner(userId)
  }
}
