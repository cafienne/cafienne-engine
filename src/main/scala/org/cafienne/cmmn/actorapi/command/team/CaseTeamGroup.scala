package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.cmmn.definition.team.CaseTeamDefinition
import org.cafienne.cmmn.instance.team.{CaseTeamError, MemberType, Team}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

import scala.jdk.CollectionConverters._

case class CaseTeamGroup(groupId: String, mappings: Seq[GroupRoleMapping] = Seq()) extends CaseTeamMember {
  override val isGroup: Boolean = true
  override val caseRoles: Set[String] = mappings.flatMap(_.caseRoles).toSet
  override val memberType: MemberType = MemberType.Group

  override def memberId: String = groupId

  override def currentMember(team: Team): CaseTeamMember = team.getGroup(groupId)

  override def validateRolesExist(caseDefinition: CaseTeamDefinition): Unit = {
    val undefinedRoles = mappings.flatMap(_.caseRoles).filter(roleName => caseDefinition.getCaseRole(roleName) == null)

    if (undefinedRoles.nonEmpty) {
      throw new CaseTeamError("ConsentGroup maps to invalid case roles: " + undefinedRoles.mkString(","))
    }
  }

  def getMappings: java.util.List[GroupRoleMapping] = {
    mappings.asJava
  }

  def differsFrom(newGroup: CaseTeamGroup): Boolean = {
    val differentMappings: Seq[GroupRoleMapping] = {
      // Their mappings that we do not have, plus our mappings that they do not have
      newGroup.mappings.filter(mapping => !this.mappings.exists(_.eq(mapping))) ++ this.mappings.filter(mapping => !newGroup.mappings.exists(_.eq(mapping)))
    }
    differentMappings.nonEmpty
  }

  def getRemovedMappings(newGroup: CaseTeamGroup): java.util.Set[GroupRoleMapping] = {
    this.mappings.filter(mapping => !newGroup.mappings.exists(_.eq(mapping))).toSet.asJava
  }

  override def toValue: Value[_] = super.toValue.asMap.plus(Fields.groupId, groupId, Fields.mappings, mappings)

  override def generateChangeEvent(team: Team, newRoles: Set[String]): Unit = team.setGroup(this.copy(mappings = this.mappings.map(m => m.copy(caseRoles = m.caseRoles.intersect(newRoles)))))
}

object CaseTeamGroup {
  def deserialize(json: ValueMap): CaseTeamGroup = {
    val groupId = json.readString(Fields.groupId)
    val mappings = json.readObjects(Fields.mappings, GroupRoleMapping.deserialize).asScala.toSeq
    CaseTeamGroup(groupId, mappings)
  }
}



