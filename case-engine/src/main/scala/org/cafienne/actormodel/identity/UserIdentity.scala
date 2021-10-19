package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

trait UserIdentity extends CafienneJson {
  val id: String

  override def toValue: Value[_] = new ValueMap(Fields.userId, id)
}

object UserIdentity {
  def deserialize(json: ValueMap): UserIdentity = {
    // For now assume it's a TenantUser
    TenantUser.deserialize(json)
  }
}
