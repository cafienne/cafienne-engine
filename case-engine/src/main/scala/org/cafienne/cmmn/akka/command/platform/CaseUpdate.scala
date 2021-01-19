package org.cafienne.cmmn.akka.command.platform

import org.cafienne.akka.actor.serialization.Fields
import org.cafienne.akka.actor.serialization.json.{Value, ValueMap}
import org.cafienne.infrastructure.json.CafienneJson

case class CaseUpdate(caseId: String, tenant: String, platformUpdate: PlatformUpdate) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.caseInstanceId, caseId, Fields.tenant, tenant, Fields.update, platformUpdate.toValue)
}

object CaseUpdate {
  def deserialize(json: ValueMap) = {
    CaseUpdate(json.raw(Fields.caseInstanceId), json.raw(Fields.tenant), PlatformUpdate.deserialize(json.withArray(Fields.update)))
  }
}


