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

package org.cafienne.userregistration.consentgroup.actorapi

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.jdk.CollectionConverters.SetHasAsJava

case class ConsentGroupMember(userId: String, roles: Set[String] = Set(), isOwner: Boolean = false) extends CafienneJson {

  def getRoles: java.util.Set[String] = roles.asJava

  override def toValue: Value[_] = {
    new ValueMap(Fields.userId, userId, Fields.isOwner, isOwner, Fields.roles, roles)
  }
}

object ConsentGroupMember {
  /**
    * Create a group member from the predefined json format
    * @param map
    * @return
    */
  def deserialize(map: ValueMap): ConsentGroupMember = {
    val userId = map.readString(Fields.userId)
    val roles = map.readStringList(Fields.roles).toSet
    val isOwner = map.readBoolean(Fields.isOwner)
    ConsentGroupMember(userId, roles, isOwner)
  }

  /**
    * Create a filled group member
    * @param userId
    * @param roles
    * @param isOwner
    * @return
    */
  def apply(userId: String, roles: Seq[String], isOwner: Boolean): ConsentGroupMember = ConsentGroupMember(userId, roles.toSet, isOwner)
}