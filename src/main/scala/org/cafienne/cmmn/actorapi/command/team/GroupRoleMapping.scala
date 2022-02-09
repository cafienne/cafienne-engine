package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import java.util
import scala.jdk.CollectionConverters.SetHasAsJava

case class GroupRoleMapping(caseRoles: Set[String], groupRole: String, isOwner: Boolean = false) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap(Fields.caseRole, caseRoles, Fields.groupRole, groupRole, Fields.isOwner,isOwner)

  def getCaseRoles: util.Set[String] = caseRoles.asJava
}

object GroupRoleMapping {
  def deserialize(json: ValueMap): GroupRoleMapping = {
    val caseRole = json.readStringList(Fields.caseRole).toSet
    val groupRole = json.readString(Fields.groupRole)
    val isOwner = json.readBoolean(Fields.isOwner)
    GroupRoleMapping(caseRole, groupRole, isOwner)
  }
}