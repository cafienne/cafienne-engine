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

package org.cafienne.persistence.querydb.materializer.cases.file

import org.cafienne.engine.cmmn.actorapi.event.file.{BusinessIdentifierCleared, BusinessIdentifierSet}
import org.cafienne.persistence.querydb.record.CaseBusinessIdentifierRecord

object CaseIdentifierMerger {

  def merge(event: BusinessIdentifierSet): CaseBusinessIdentifierRecord = {
    CaseBusinessIdentifierRecord(event.getActorId, event.tenant, event.name, Some(event.value.getValue.toString), true, event.path.toString)
  }

  def merge(event: BusinessIdentifierCleared): CaseBusinessIdentifierRecord = {
    CaseBusinessIdentifierRecord(event.getActorId, event.tenant, event.name, None, false, event.path.toString)
  }
}
