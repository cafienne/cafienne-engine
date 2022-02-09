package org.cafienne.consentgroup.actorapi

import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.jdk.CollectionConverters.CollectionHasAsScala

case class ConsentGroup(id: String, tenant: String, members: Seq[ConsentGroupMember]) extends CafienneJson {

  override def toValue: Value[_] = {
    new ValueMap(Fields.groupId, id, Fields.tenant, tenant, Fields.members, Value.convert(members.map(_.toValue)))
  }

  /**
    * Returns the set of roles in this group (by collecting roles of each user and mapping all those to a set)
    */
  lazy val groupRoles: Set[String] = members.flatMap(_.roles).toSet
}

object ConsentGroup {
  def deserialize(map: ValueMap): ConsentGroup = {
    val id: String = map.get(Fields.groupId).toString
    val tenant: String = map.get(Fields.tenant).toString
    val members = map.readObjects(Fields.members, ConsentGroupMember.deserialize).asScala.toSeq
    ConsentGroup(id, tenant, members)
  }
}