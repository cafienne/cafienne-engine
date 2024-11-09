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

package org.cafienne.cmmn.actorapi.command.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import org.cafienne.cmmn.actorapi.event.team.deprecated.member.{CaseOwnerAdded, CaseOwnerRemoved, TeamRoleCleared, TeamRoleFilled}
import org.cafienne.cmmn.actorapi.event.team.deprecated.user.{TeamMemberAdded, TeamMemberRemoved}
import org.cafienne.cmmn.definition.team.CaseRoleDefinition
import org.cafienne.cmmn.instance.team.{MemberType, Team}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._

import java.util
import scala.jdk.CollectionConverters._

trait CaseTeamUser extends CaseTeamMember {
  override val isUser: Boolean = true
  override val memberType: MemberType = MemberType.User
  override val rolesRemoved: Set[String] = Set()
  val userId: String
  val origin: Origin

  override def memberId: String = userId

  override def currentMember(team: Team): CaseTeamMember = team.getUser(memberId)

  override def toValue: Value[_] = {
    val json = memberKeyJson.plus(Fields.isOwner, isOwner, Fields.caseRoles, caseRoles)
    jsonPlusOptionalField(json, Fields.rolesRemoved, rolesRemoved)
  }

  override def memberKeyJson: ValueMap = new ValueMap(Fields.userId, userId, Fields.origin, origin)

  def extend(caseRole: String): CaseTeamUser = this.copy(extraRoles = Set(caseRole))

  override def generateChangeEvent(team: Team, newRoles: Set[String]): Unit = team.setUser(this.copy(newRoles = newRoles))

  def differsFrom(that: CaseTeamUser): Boolean = {
    !(this.userId.equals(that.userId) &&
      this.isOwner == that.isOwner &&
      this.caseRoles.diff(that.caseRoles).isEmpty && that.caseRoles.diff(this.caseRoles).isEmpty)
  }

  def minus(existingUserInfo: CaseTeamUser): CaseTeamUser = {
    val removedRoles = existingUserInfo.caseRoles.diff(caseRoles)
    this.copy(newRolesRemoved = removedRoles)
  }

  def cloneUser(newUserId: String): CaseTeamUser = this.copy(newUserId = newUserId)

  /**
    * Copy method sort of resembles scala case class copy, but traits do not have that.
    * This method can also be to add extra roles
    */
  def copy(newUserId: String = this.userId, newRoles: Set[String] = this.caseRoles, extraRoles: Set[String] = Set(), newOrigin: Origin = this.origin, newOwnership: Boolean = this.isOwner, newRolesRemoved: Set[String] = this.rolesRemoved): CaseTeamUser =
    CaseTeamUser.from(userId = newUserId, origin = newOrigin, caseRoles = newRoles ++ extraRoles, isOwner = newOwnership, rolesRemoved = newRolesRemoved)
}

object CaseTeamUser extends LazyLogging {
  def create(newUser: CaseUserIdentity, role: CaseRoleDefinition): CaseTeamUser =
    CaseTeamUser.from(userId = newUser.id, origin = newUser.origin, caseRoles = {
      if (role == null) Set()
      else Set(role.getName)
    })

  def from(userId: String, origin: Origin, caseRoles: Set[String] = Set(), isOwner: Boolean = false, rolesRemoved: Set[String] = Set()): CaseTeamUser = {
    val props = (userId, origin, caseRoles, isOwner, rolesRemoved) // Convert to props, as the inline implementation otherwise does not take the arguments of our function
    new CaseTeamUser {
      override val userId: String = props._1
      override val origin: Origin = props._2
      override val caseRoles: Set[String] = props._3
      override val isOwner: Boolean = props._4
      override val rolesRemoved: Set[String] = props._5
    }
  }

  //  def createTestUser(user: TenantUser, isOwner: Boolean) = new CaseTeamUser(userId = user.id, origin = Origin.Tenant, caseRoles = user.roles, isOwner = isOwner)

  def getDeserializer(parent: ValueMap): CaseTeamMemberDeserializer[CaseTeamUser] = {
    (json: ValueMap) =>  {
      // Special deserializer that can migrate rolesRemoved from parent json for older event format (Cafienne versions 1.1.16 - 1.1.18)
      if (parent.has(Fields.rolesRemoved)) {
        parent.withArray(Fields.rolesRemoved).getValue.asScala.foreach(json.withArray(Fields.rolesRemoved).add(_))
      }
      CaseTeamUser.deserialize(json)
    }
  }

  def deserialize(json: ValueMap): CaseTeamUser = {
    CaseTeamUser.from(
      userId = json.readString(Fields.userId),
      origin = json.readEnum(Fields.origin, classOf[Origin]),
      caseRoles = json.readStringList(Fields.caseRoles).toSet,
      isOwner = json.readBoolean(Fields.isOwner),
      rolesRemoved = json.readStringList(Fields.rolesRemoved).toSet)
  }

  def handleDeprecatedUserEvent(users: util.Map[String, CaseTeamUser], event: DeprecatedCaseTeamEvent): Unit = {
    def getUser: CaseTeamUser = users.get(event.memberId)

    def put(user: CaseTeamUser): Unit = users.put(user.memberId, user)

    event match {
      case _: CaseOwnerAdded => put(getUser.copy(newOwnership = true))
      case _: CaseOwnerRemoved => put(getUser.copy(newOwnership = false))
      case event: TeamRoleCleared =>
        if (event.isMemberItself) users.remove(event.memberId)
        else put({
          val user = getUser
          user.copy(newRoles = user.caseRoles -- Set(event.roleName()))
        })
      case event: TeamRoleFilled =>
        if (event.isMemberItself) put(CaseTeamUser.from(userId = event.memberId, origin = Origin.Tenant))
        else put(getUser.copy(extraRoles = Set(event.roleName())))
      case event: TeamMemberAdded => put(
        // Classic initial team event. Only CaseTeamUsers supported, not yet tenant roles.
        //  All these users belong to the tenant, and all of them are case owner
        CaseTeamUser.from(userId = event.memberId, origin = Origin.Tenant, caseRoles = event.getRoles.asScala.toSet, isOwner = true)
      )
      case event: TeamMemberRemoved => users.remove(event.memberId)
      case other => logger.warn(s"Unexpected deprecated case team event of type ${other.getClass.getName}")
    }
  }
}
