package org.cafienne.service.api.cases

import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.instance.casefile.{Value, ValueList, ValueMap}
import org.cafienne.infrastructure.json.CafienneJson
import org.cafienne.service.api.cases.table.{CaseFileRecord, CaseRecord, PlanItemHistoryRecord, PlanItemRecord}

final case class FullCase(caseInstance: CaseRecord, file: CaseFile, team: CaseTeam, planitems: CasePlan) extends CafienneJson {
  override def toValue: Value[_] = caseInstance.toValue.merge(new ValueMap("team", team.toValue(), "file", file.toValue(), "planitems", planitems.toValue))
}

final case class CasePlan(items: Seq[PlanItemRecord]) extends CafienneJson {
  override def toValue() = {
    val list = new ValueList
    items.foreach(item => list.add(item.toValueMap))
    list
  }
}

final case class CaseFile(record: CaseFileRecord) extends CafienneJson {
  def toValue(): ValueMap = {
    if (record == null) {
      new ValueMap
    } else {
      record.toValueMap
    }
  }
}

final case class PlanItem(record: PlanItemRecord) extends CafienneJson {
  override def toValue: Value[_] = record.toValueMap
}

final case class PlanItemHistory(records: Seq[PlanItemHistoryRecord]) extends CafienneJson {
  override def toValue = Value.convert(records.map(item => item.toValue))
}
