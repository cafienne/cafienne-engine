package org.cafienne.cmmn.actorapi.command.platform

import org.cafienne.actormodel.command.exception.InvalidCommandException
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
