package org.cafienne.consentgroup.actorapi

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