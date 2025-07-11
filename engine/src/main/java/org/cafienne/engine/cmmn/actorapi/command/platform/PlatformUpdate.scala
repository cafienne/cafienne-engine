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

package org.cafienne.engine.cmmn.actorapi.command.platform

import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.json.{CafienneJson, Value, ValueList}

case class PlatformUpdate(info: Seq[NewUserInformation]) extends CafienneJson {
  override def toValue: Value[_] = {
    val list = new ValueList()
    info.map(user => list.add(user.toValue))
    list
  }

  def validate() = {
    val existingUserIds = info.map(u => u.existingUserId)
    // If the list of existing id's is as long as the set it is consisting of unique identifiers.
    //  If not, then the list is not good, as the same old id might get changed into different new ids.
    //  We're not checking for duplicates here!
    if (existingUserIds.size != existingUserIds.toSet.size) {
      throw new InvalidCommandException("An existing user id cannot be updated into multiple new user ids")
    }
  }

  /** Returns new user info for the specified id if it is present, or else null */
  def getUserUpdate(userId: String): NewUserInformation = info.find(i => i.existingUserId == userId).getOrElse(null)
}

object PlatformUpdate {
  def deserialize(list: ValueList): PlatformUpdate = {
    val users = scala.collection.mutable.Buffer[NewUserInformation]()
    list.forEach(map => users += NewUserInformation(map.asMap().raw("existingUserId"), map.asMap().raw("newUserId")))
    PlatformUpdate(users.toSeq)
  }
}
