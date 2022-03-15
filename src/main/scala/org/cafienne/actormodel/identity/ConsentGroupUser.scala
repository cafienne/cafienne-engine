package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}

case class ConsentGroupUser(id: String, groupId: String, tenant: String) extends UserIdentity {
  override def toValue: Value[_] = new ValueMap(Fields.userId, id, Fields.groupId, groupId, Fields.tenant, tenant)
}

object ConsentGroupUser {
  def deserialize(json: ValueMap): ConsentGroupUser = {
    ConsentGroupUser(
      id = json.readString(Fields.userId),
      groupId = json.readString(Fields.groupId),
      tenant = json.readString(Fields.tenant)
    )
  }
}
