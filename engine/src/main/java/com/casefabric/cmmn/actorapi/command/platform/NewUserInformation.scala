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

package com.casefabric.cmmn.actorapi.command.platform

import com.casefabric.actormodel.identity.TenantUser
import com.casefabric.json.{CaseFabricJson, Value, ValueList, ValueMap}

import java.util

case class NewUserInformation(existingUserId: String, newUserId: String) extends CaseFabricJson{
  override def toValue: Value[_] = new ValueMap("existingUserId", existingUserId, "newUserId", newUserId)

  def copyTo(tenantUser: TenantUser): TenantUser = tenantUser.copy(id = newUserId)
}

object NewUserInformation {
  def deserialize(list: ValueList): java.util.List[NewUserInformation] = {
    val users = new util.ArrayList[NewUserInformation]()
    list.forEach(map => users.add(NewUserInformation(map.asMap().raw("existingUserId"), map.asMap().raw("newUserId"))))
    users
  }
}
