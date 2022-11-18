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

package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import java.util
import scala.jdk.CollectionConverters.SetHasAsJava

case class GroupRoleMapping(groupRole: String, isOwner: Boolean = false, caseRoles: Set[String], rolesRemoved: Set[String] = Set()) extends CafienneJson {
  override def toValue: Value[_] = {
    val json = new ValueMap(Fields.groupRole, groupRole, Fields.isOwner, isOwner, Fields.caseRoles, caseRoles)
    if (rolesRemoved.nonEmpty) {
      json.plus(Fields.rolesRemoved, rolesRemoved)
    }
    json
  }

  def getCaseRoles: util.Set[String] = caseRoles.asJava
}

object GroupRoleMapping {
  def deserialize(json: ValueMap): GroupRoleMapping = {
    val groupRole = json.readString(Fields.groupRole)
    val isOwner = json.readBoolean(Fields.isOwner)
    val caseRoles = json.readStringList(Fields.caseRoles).toSet
    val rolesRemoved = json.readStringList(Fields.rolesRemoved).toSet
    GroupRoleMapping(groupRole = groupRole, isOwner = isOwner, caseRoles = caseRoles, rolesRemoved = rolesRemoved)
  }
}
