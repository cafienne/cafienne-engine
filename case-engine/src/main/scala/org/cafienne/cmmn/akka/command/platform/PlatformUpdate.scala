package org.cafienne.cmmn.akka.command.platform

import org.cafienne.akka.actor.serialization.json.{Value, ValueList}
import org.cafienne.infrastructure.json.CafienneJson

case class PlatformUpdate(info: Seq[NewUserInformation]) extends CafienneJson {
  override def toValue: Value[_] = {
    val list = new ValueList()
    info.map(user => list.add(user.toValue))
    list
  }

  def filter(userIds: ValueList): PlatformUpdate = {
    val list = scala.collection.mutable.Buffer[String]()
    userIds.getValue.forEach(v => list += v.getValue.toString)
    new PlatformUpdate(info.filter(user => list.contains(user.existingUserId)))
  }
}

object PlatformUpdate {
  def deserialize(list: ValueList): PlatformUpdate = {
    val users = scala.collection.mutable.Buffer[NewUserInformation]()
    list.forEach(map => users += NewUserInformation(map.asMap().raw("existingUserId"), map.asMap().raw("newUserId")))
    PlatformUpdate(users)
  }


}
