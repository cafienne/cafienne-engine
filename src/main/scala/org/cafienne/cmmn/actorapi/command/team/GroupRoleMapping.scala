package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import java.util
import scala.jdk.CollectionConverters.SetHasAsJava

case class GroupRoleMapping(groupRole: String, isOwner: Boolean = false, caseRoles: Set[String], rolesRemoved: Set[String] = Set()) extends CafienneJson {
  override def toValue: Value[_] = {
    val json = new ValueMap(Fields.groupRole, groupRole, Fields.isOwner, isOwner, Fields.caseRoles, caseRoles)
    if (rolesRemoved.nonEmpty) {
      json.plus(Fields.rolesRemoved, rolesRemoved)
    }
    json
  }

  def getCaseRoles: util.Set[String] = caseRoles.asJava
}

object GroupRoleMapping {
  def deserialize(json: ValueMap): GroupRoleMapping = {
    val groupRole = json.readString(Fields.groupRole)
    val isOwner = json.readBoolean(Fields.isOwner)
    val caseRoles = json.readStringList(Fields.caseRoles).toSet
    val rolesRemoved = json.readStringList(Fields.rolesRemoved).toSet
    GroupRoleMapping(groupRole = groupRole, isOwner = isOwner, caseRoles = caseRoles, rolesRemoved = rolesRemoved)
  }
}
