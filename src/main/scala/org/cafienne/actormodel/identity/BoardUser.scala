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

package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{Value, ValueMap}

case class BoardUser(id: String, boardId: String) extends UserIdentity {
  override def toValue: Value[_] = new ValueMap(Fields.userId, id, Fields.boardId, boardId)

  override def asCaseUserIdentity(): CaseUserIdentity = CaseUserIdentity(id, Origin.IDP)

  val isOwner = true
}

object BoardUser {
  def deserialize(json: ValueMap): BoardUser = {
    BoardUser(
      id = json.readString(Fields.userId),
      boardId = json.readString(Fields.boardId)
    )
  }
}
