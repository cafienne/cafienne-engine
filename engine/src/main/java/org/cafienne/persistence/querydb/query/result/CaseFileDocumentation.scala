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

package org.cafienne.persistence.querydb.query.result

import org.cafienne.cmmn.definition.casefile.{CaseFileItemCollectionDefinition, CaseFileItemDefinition}
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}
import org.cafienne.persistence.querydb.record.CaseDefinitionRecord

final case class CaseFileDocumentation(record: CaseDefinitionRecord) extends CafienneJson {
  private def docs = (item: CaseFileItemDefinition) => Documentation(item.documentation.text, item.documentation.textFormat).toValue

  def extendList(list: ValueList, collection: CaseFileItemCollectionDefinition): ValueList = {
    collection.getChildren.forEach(item => {
      if (!item.documentation.text.isBlank) {
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
