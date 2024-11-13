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

package com.casefabric.querydb.query

import com.casefabric.json.{CaseFabricJson, Value, ValueList, ValueMap}
import com.casefabric.querydb.record.CaseBusinessIdentifierRecord

final case class IdentifierSet(records: Seq[CaseBusinessIdentifierRecord]) extends CaseFabricJson {
  override def toValue: Value[_] = {
    val list = new ValueList
    records.foreach(record => list.add(new ValueMap("caseInstanceId", record.caseInstanceId, "tenant", record.tenant, "name", record.name, "value", record.value.orNull)))
    list
  }
}

