package org.cafienne.service.api.cases

import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.instance.casefile.{ValueList, ValueMap}
import org.cafienne.service.api.cases.table.{CaseFileRecord, CaseRecord, PlanItemHistoryRecord, PlanItemRecord}

final case class FullCase(caseInstance: CaseRecord, file: CaseFile, team: CaseTeam, planitems: CasePlan) {
  override def toString: String = {
    caseInstance.toValueMap.merge(new ValueMap("team", team.toValue(), "file", file.toJson(), "planitems", planitems.toJson)).toString
  }
}

final case class CasePlan(items: Seq[PlanItemRecord]) {
  def toJson(): ValueList = {
    val list = new ValueList
    items.foreach(item => list.add(item.toValueMap))
    list
  }

  override def toString: String = {
    toJson.toString
  }
}

final case class CaseFile(record: CaseFileRecord) {
  def toJson() : ValueMap = {
    if (record == null) {
      new ValueMap
    } else {
      record.toValueMap
    }
  }

  override def toString: String = {
    toJson.toString
  }
}

final case class PlanItem(record: PlanItemRecord) {
  override def toString: String = record.toValueMap.toString
}

final case class PlanItemHistory(records: Seq[PlanItemHistoryRecord]) {
  def toJson(): ValueList = {
    val responseValues = new ValueList
    records.foreach(item => responseValues.add(item.toValueMap))
    responseValues
  }
  override def toString: String = {
    toJson.toString
  }
}
