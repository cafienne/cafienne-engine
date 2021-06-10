package org.cafienne.cmmn.actorapi.command.platform

import org.cafienne.akka.actor.serialization.json.{Value, ValueList}
import org.cafienne.infrastructure.json.CafienneJson

case class PlatformUpdate(info: Seq[NewUserInformation]) extends CafienneJson {
  override def toValue: Value[_] = {
    val list = new ValueList()
    info.map(user => list.add(user.toValue))
    list
  }

  /** Returns new user info for the specified id if it is present, or else null */
  def getUserUpdate(userId: String): NewUserInformation = info.find(i => i.existingUserId == userId).getOrElse(null)
}

object PlatformUpdate {
  def deserialize(list: ValueList): PlatformUpdate = {
    val users = scala.collection.mutable.Buffer[NewUserInformation]()
    list.forEach(map => users += NewUserInformation(map.asMap().raw("existingUserId"), map.asMap().raw("newUserId")))
    PlatformUpdate(users)
  }
}
