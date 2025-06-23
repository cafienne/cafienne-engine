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

package org.cafienne.persistence.querydb.schema.table

import org.cafienne.persistence.infrastructure.jdbc.SlickTableExtensions

final case class CaseIdentifierRecord(id: String, parentCaseId: String, rootCaseId: String)

trait Views extends SlickTableExtensions {

  import dbConfig.profile.api._

  class CaseIdentifierView(tag: Tag) extends CafienneTable[CaseIdentifierRecord](tag, "case_instance") {
    lazy val id: Rep[String] = idColumn[String]("id", O.PrimaryKey)

    lazy val parentCaseId: Rep[String] = idColumn[String]("parent_case_id")

    lazy val rootCaseId: Rep[String] = idColumn[String]("root_case_id")

    lazy val * = (id, parentCaseId, rootCaseId).mapTo[CaseIdentifierRecord]
  }
}
