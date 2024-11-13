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

package com.casefabric.actormodel.identity

import com.casefabric.infrastructure.serialization.Fields
import com.casefabric.json.{Value, ValueMap}

import scala.jdk.CollectionConverters.CollectionHasAsScala

trait CaseUserIdentity extends UserIdentity {
  // user id
  val id: String
  // user origin (IDP, Tenant, Platform, etc.)
  val origin: Origin
  // tenant roles this user has, defaults to empty
  val tenantRoles: Set[String] = Set()
  // groups that the user is member of, defaults to empty
  val groups: Seq[ConsentGroupMembership] = Seq()

  override def toValue: Value[_] = {
    super.toValue.asMap().plus(Fields.origin, origin, Fields.tenantRoles, tenantRoles, Fields.groups, groups)
  }

  override def asCaseUserIdentity(): CaseUserIdentity = this
}

object CaseUserIdentity {
  def apply(user: String, background: Origin): CaseUserIdentity = {
    new CaseUserIdentity {
      override val id: String = user
      override val origin: Origin = background
    }
  }

  def deserialize(json: ValueMap): CaseUserIdentity = {
    new CaseUserIdentity {
      override val id: String = json.readString(Fields.userId)
      override val origin: Origin = json.readEnum(Fields.origin, classOf[Origin])
      override val groups: Seq[ConsentGroupMembership] = json.readObjects(Fields.groups, ConsentGroupMembership.deserialize).asScala.toSeq
      override val tenantRoles: Set[String] = json.readStringList(Fields.tenantRoles).toSet
    }
  }
}
