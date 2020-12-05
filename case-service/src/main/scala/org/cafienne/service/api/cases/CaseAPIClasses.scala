package org.cafienne.service.api.cases

import org.cafienne.akka.actor.serialization.json.{Value, ValueList, ValueMap}
import org.cafienne.cmmn.akka.command.team.CaseTeam
import org.cafienne.cmmn.definition.casefile.{CaseFileItemCollectionDefinition, CaseFileItemDefinition}
import org.cafienne.infrastructure.json.CafienneJson
import org.cafienne.service.api.projection.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, PlanItemHistoryRecord, PlanItemRecord}

final case class FullCase(caseInstance: CaseRecord, file: CaseFile, team: CaseTeam, planitems: CasePlan) extends CafienneJson {
  override def toValue: Value[_] = caseInstance.toValue.merge(new ValueMap("team", team.members, "file", file.toValue, "planitems", planitems.toValue))
}

final case class CaseDefinitionDocument(record: CaseDefinitionRecord) {
  def xml: String = record.content
}

final case class CasePlan(items: Seq[PlanItemRecord]) extends CafienneJson {
  override def toValue: Value[_] = {
    val list = new ValueList
    items.foreach(item => list.add(item.toValueMap))
    list
  }
}

final case class CaseFile(record: CaseFileRecord) extends CafienneJson {
  override def toValue: Value[_] = {
    if (record == null) {
      new ValueMap
    } else {
      record.toValueMap
    }
  }
}

final case class CaseFileDocumentation(record: CaseDefinitionRecord) extends CafienneJson {
  def docs = (item: CaseFileItemDefinition) => Documentation(item.documentation.text, item.documentation.textFormat).toValue

  def extendList(list: ValueList, collection: CaseFileItemCollectionDefinition): ValueList = {
    collection.getChildren.forEach(item => {
      if (! item.documentation.text.isBlank) {
        list.add(new ValueMap("path", item.getPath, "documentation", docs(item)))
      }
      extendList(list, item)
    })
    list
  }

  override def toValue: Value[_] = {
    val list = new ValueList
    extendList(list, record.caseDefinition.getCaseFileModel)
  }
}

final case class PlanItem(record: PlanItemRecord) extends CafienneJson {
  override def toValue: Value[_] = record.toValueMap
}

final case class Documentation(text: String, textFormat: String = "text/plain") extends CafienneJson {
  override def toValue: Value[_] = {
    text.isBlank match {
      case true => Value.NULL
      case false => new ValueMap("textFormat", textFormat, "text", text)
    }
  }
}

final case class PlanItemHistory(records: Seq[PlanItemHistoryRecord]) extends CafienneJson {
  override def toValue: Value[_] = Value.convert(records.map(item => item.toValue))
}
