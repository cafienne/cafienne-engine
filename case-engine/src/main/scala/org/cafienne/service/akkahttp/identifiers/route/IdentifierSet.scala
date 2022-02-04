package org.cafienne.service.akkahttp.identifiers.route

import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}
import org.cafienne.service.db.record.CaseBusinessIdentifierRecord

final case class IdentifierSet(records: Seq[CaseBusinessIdentifierRecord]) extends CafienneJson {
  override def toValue: Value[_] = {
    val list = new ValueList
    records.foreach(record => list.add(new ValueMap("caseInstanceId", record.caseInstanceId, "tenant", record.tenant, "name", record.name, "value", record.value.orNull)))
    list
  }
}

