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

package com.casefabric.cmmn.actorapi.command.team

import com.casefabric.cmmn.definition.team.CaseTeamDefinition
import com.casefabric.cmmn.instance.team.{CaseTeamError, MemberType, Team}
import com.casefabric.json._

import java.util
import scala.jdk.CollectionConverters._

trait CaseTeamMember extends CaseFabricJson {
  val caseRoles: Set[String] = Set()
  val rolesRemoved: Set[String] = Set()
  val isOwner: Boolean = false

  val isUser = false
  val isTenantRole = false
  val isGroup = false

  val memberType: MemberType
  def memberId: String

  lazy val description: String = s"$memberType - $memberId"

  def currentMember(team: Team): CaseTeamMember

  def memberKeyJson: ValueMap

  /**
    * Add the field to the json with the list, but only if the list has elements.
    */
  def jsonPlusOptionalField(json: ValueMap, fieldName: AnyRef, list: Iterable[_]): ValueMap = {
    if (list.nonEmpty) {
      json.plus(fieldName, list)
    } else {
      json
    }
  }

  def validateRolesExist(definition: CaseTeamDefinition): Unit = {
    val allRolesUnderProcessing = caseRoles
    val blankRoles = allRolesUnderProcessing.filter(roleName => roleName.isBlank)
    val undefinedRoles = allRolesUnderProcessing.filter(roleName => definition.getCaseRole(roleName) == null)

    if (blankRoles.nonEmpty || undefinedRoles.nonEmpty) {
      if (undefinedRoles.isEmpty) {
        throw new CaseTeamError("An empty role is not permitted")
      } else {
        throw new CaseTeamError("The following role(s) are not defined in the case: " + undefinedRoles.mkString(","))
      }
    }
  }

  def generateChangeEvent(team: Team, newRoles: Set[String]): Unit

  def migrateRoles(team: Team, changedRoleNames: util.Map[String, String], droppedRoles: util.Set[String]): Unit = {
    val newRoles = this.caseRoles.filterNot(droppedRoles.contains(_)).map(role =>changedRoleNames.getOrDefault(role, role))
    generateChangeEvent(team, newRoles)
  }

  def getCaseRoles: util.Set[String] = caseRoles.asJava
}
