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

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class ConsentGroup(id: String, tenant: String, members: Seq[ConsentGroupMember]) extends CafienneJson {

  override def toValue: Value[_] = {
    new ValueMap(Fields.groupId, id, Fields.tenant, tenant, Fields.members, Value.convert(members.map(_.toValue)))
  }

  /**
    * Returns the set of roles in this group (by collecting roles of each user and mapping all those to a set)
    */
  lazy val groupRoles: Set[String] = members.flatMap(_.roles).toSet
}

object ConsentGroup {
  def deserialize(map: ValueMap): ConsentGroup = {
    val id: String = map.get(Fields.groupId).toString
    val tenant: String = map.get(Fields.tenant).toString
    val members = map.readObjects(Fields.members, ConsentGroupMember.deserialize).asScala.toSeq
    ConsentGroup(id, tenant, members)
  }
}