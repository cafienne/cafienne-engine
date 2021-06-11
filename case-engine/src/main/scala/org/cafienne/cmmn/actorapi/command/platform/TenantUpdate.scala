package org.cafienne.cmmn.actorapi.command.platform

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}
import org.cafienne.json.CafienneJson

case class TenantUpdate(tenant: String, platformUpdate: PlatformUpdate) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.tenant, tenant, Fields.update, platformUpdate.toValue)
}

object TenantUpdate {
  def deserialize(json: ValueMap) = {
    TenantUpdate(json.raw(Fields.tenant), PlatformUpdate.deserialize(json.withArray(Fields.update)))
  }
}





