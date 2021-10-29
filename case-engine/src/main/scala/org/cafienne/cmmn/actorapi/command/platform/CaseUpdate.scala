package org.cafienne.cmmn.actorapi.command.platform

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

case class CaseUpdate(caseId: String, tenant: String, platformUpdate: PlatformUpdate) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.caseInstanceId, caseId, Fields.tenant, tenant, Fields.update, platformUpdate.toValue)
}

object CaseUpdate {
  def deserialize(json: ValueMap) = {
    CaseUpdate(json.readString(Fields.caseInstanceId), json.readString(Fields.tenant), PlatformUpdate.deserialize(json.withArray(Fields.update)))
  }
}


