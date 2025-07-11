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

package org.cafienne.engine.cmmn.actorapi.command.team

import org.cafienne.engine.cmmn.definition.team.CaseTeamDefinition
import org.cafienne.engine.cmmn.instance.team.{CaseTeamError, MemberType, Team}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

import java.util
import scala.jdk.CollectionConverters._

case class CaseTeamGroup(groupId: String, mappings: Seq[GroupRoleMapping] = Seq(), removedMappings: Seq[GroupRoleMapping] = Seq()) extends CaseTeamMember {
  override val isGroup: Boolean = true
  override val caseRoles: Set[String] = mappings.flatMap(_.caseRoles).toSet
  override val memberType: MemberType = MemberType.Group
  override val isOwner: Boolean = mappings.exists(_.isOwner)

  override def memberId: String = groupId

  override def currentMember(team: Team): CaseTeamMember = team.getGroup(groupId)

  override def validateRolesExist(caseDefinition: CaseTeamDefinition): Unit = {
    val undefinedRoles = mappings.flatMap(_.caseRoles).filter(roleName => caseDefinition.getCaseRole(roleName) == null)

    if (undefinedRoles.nonEmpty) {
      throw new CaseTeamError("ConsentGroup maps to invalid case roles: " + undefinedRoles.mkString(","))
    }
  }

  def differsFrom(newGroup: CaseTeamGroup): Boolean = {
    val differentMappings: Seq[GroupRoleMapping] = {
      // Their mappings that we do not have, plus our mappings that they do not have
      newGroup.mappings.filter(mapping => !this.mappings.exists(_.equals(mapping))) ++ this.mappings.filter(mapping => !newGroup.mappings.exists(_.equals(mapping)))
    }
    ! (this.groupId.equals(newGroup.groupId)
      && differentMappings.isEmpty)
  }

  def minus(existingGroup: CaseTeamGroup): CaseTeamGroup = {
    def updateWithRemovedCaseRoles(newMapping: GroupRoleMapping): GroupRoleMapping = {
      // Note, if existing mapping is not found, simply use the new mapping. That will then not have removed roles.
      val existingMapping = existingGroup.mappings.find(_.groupRole == newMapping.groupRole).getOrElse(newMapping)
      val removedCaseRoles = existingMapping.caseRoles.diff(newMapping.caseRoles)
      newMapping.copy(rolesRemoved = removedCaseRoles)
    }
    val updatedMappings = this.mappings.map(updateWithRemovedCaseRoles)

    val removedMappings = existingGroup.mappings.filterNot(mapping => this.mappings.exists(_.groupRole == mapping.groupRole))
    this.copy(mappings = updatedMappings, removedMappings = removedMappings)
  }

  override def toValue: Value[_] = {
    val json = memberKeyJson.plus(Fields.mappings, mappings)
    jsonPlusOptionalField(json, Fields.removedMappings, removedMappings)
  }

  override def memberKeyJson: ValueMap = new ValueMap(Fields.groupId, groupId)

  override def generateChangeEvent(team: Team, newRoles: Set[String]): Unit = team.setGroup(this.copy(mappings = this.mappings.map(m => m.copy(caseRoles = m.caseRoles.intersect(newRoles)))))

  def getRemovedMappings: util.List[GroupRoleMapping] = removedMappings.asJava
}

object CaseTeamGroup {
  def deserialize(json: ValueMap): CaseTeamGroup = {
    val groupId = json.readString(Fields.groupId)
    val mappings = json.readObjects(Fields.mappings, GroupRoleMapping.deserialize).asScala.toSeq
    val removedMappings = json.readObjects(Fields.removedMappings, GroupRoleMapping.deserialize).asScala.toSeq

    CaseTeamGroup(groupId, mappings, removedMappings)
  }
}



