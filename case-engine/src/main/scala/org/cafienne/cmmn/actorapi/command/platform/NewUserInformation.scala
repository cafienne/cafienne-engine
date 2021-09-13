package org.cafienne.cmmn.actorapi.command.platform

import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}

import java.util

case class NewUserInformation(existingUserId: String, newUserId: String) extends CafienneJson{
  override def toValue: Value[_] = new ValueMap("existingUserId", existingUserId, "newUserId", newUserId)
}

object NewUserInformation {
  def deserialize(list: ValueList): java.util.List[NewUserInformation] = {
    val users = new util.ArrayList[NewUserInformation]()
    list.forEach(map => users.add(NewUserInformation(map.asMap().raw("existingUserId"), map.asMap().raw("newUserId"))))
    users
  }
}
