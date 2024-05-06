/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.service.akkahttp.cases

import org.cafienne.cmmn.actorapi.command.team.CaseTeam
import org.cafienne.cmmn.definition.casefile.{CaseFileItemCollectionDefinition, CaseFileItemDefinition}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}
import org.cafienne.querydb.record._

final case class FullCase(caseInstance: CaseRecord, file: Option[CaseFileRecord], team: CaseTeamResponse, planitems: Seq[PlanItemRecord], identifiers: Seq[CaseBusinessIdentifierRecord]) extends CafienneJson {
  override def toValue: Value[_] = caseInstance.toValue.merge(new ValueMap("team", team, "file", file.getOrElse(new ValueMap()), "planitems", planitems, "identifiers", identifiers))
}

final case class CaseTeamResponse(team: CaseTeam, caseRoles: Seq[String] = Seq(),
                                  unassignedRoles: Seq[String] = Seq()) extends CafienneJson {
  override def toValue: Value[_] = team.toValue.asMap.plus(Fields.caseRoles, caseRoles, Fields.unassignedRoles, unassignedRoles)
}



final case class CaseFileDocumentation(record: CaseDefinitionRecord) extends CafienneJson {
  private def docs = (item: CaseFileItemDefinition) => Documentation(item.documentation.text, item.documentation.textFormat).toValue

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
