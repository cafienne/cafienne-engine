package org.cafienne.service.api.identifiers.route

import org.cafienne.json.{Value, ValueList, ValueMap}
import org.cafienne.json.ValueList
import org.cafienne.cmmn.actorapi.command.team.CaseTeam
import org.cafienne.cmmn.definition.casefile.{CaseFileItemCollectionDefinition, CaseFileItemDefinition}
import org.cafienne.json.CafienneJson
import org.cafienne.service.db.record.{CaseBusinessIdentifierRecord, CaseDefinitionRecord, CaseFileRecord, CaseRecord, PlanItemHistoryRecord, PlanItemRecord}

final case class IdentifierSet(records: Seq[CaseBusinessIdentifierRecord]) extends CafienneJson {
  override def toValue(): Value[_] = {
    val list = new ValueList
    records.foreach(record => list.add(new ValueMap("caseInstanceId", record.caseInstanceId, "tenant", record.tenant, "name", record.name, "value", record.value.getOrElse(null))))
    list
  }
}

