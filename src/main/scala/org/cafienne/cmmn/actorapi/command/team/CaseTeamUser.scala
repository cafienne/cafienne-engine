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
import scala.jdk.CollectionConverters.SetHasAsScala

trait CaseTeamUser extends CaseTeamMember {
  override val isUser: Boolean = true
  override val memberType: MemberType = MemberType.User
  val userId: String
  val origin: Origin

  override def memberId: String = userId

  override def currentMember(team: Team): CaseTeamMember = team.getUser(memberId)

  override def toValue: Value[_] = super.toValue.asMap().plus(Fields.userId, userId, Fields.origin, origin)

  def extend(caseRole: String): CaseTeamUser = this.copy(extraRoles = Set(caseRole))

  override def generateChangeEvent(team: Team, newRoles: Set[String]): Unit = team.setUser(this.copy(newRoles = newRoles))

  def cloneUser(newUserId: String): CaseTeamUser = this.copy(newUserId = newUserId)

  /**
    * Copy method sort of resembles scala case class copy, but traits do not have that.
    * This method can also be to add extra roles
    */
  def copy(newUserId: String = this.userId, newRoles: Set[String] = this.caseRoles, extraRoles: Set[String] = Set(), newOrigin: Origin = this.origin, newOwnership: Boolean = this.isOwner): CaseTeamUser =
    CaseTeamUser.from(userId = newUserId, origin = newOrigin, caseRoles = newRoles ++ extraRoles, isOwner = newOwnership)
}

object CaseTeamUser extends LazyLogging {
  def create(newUser: CaseUserIdentity, role: CaseRoleDefinition): CaseTeamUser =
    CaseTeamUser.from(userId = newUser.id, origin = newUser.origin, caseRoles = {
      if (role == null) Set()
      else Set(role.getName)
    })

  def from(userId: String, origin: Origin, caseRoles: Set[String] = Set(), isOwner: Boolean = false): CaseTeamUser = {
    val props = (userId, origin, caseRoles, isOwner) // Convert to props, as the inline implementation otherwise does not take the arguments of our function
    new CaseTeamUser {
      override val userId: String = props._1
      override val origin: Origin = props._2
      override val caseRoles: Set[String] = props._3
      override val isOwner: Boolean = props._4
    }
  }

  //  def createTestUser(user: TenantUser, isOwner: Boolean) = new CaseTeamUser(userId = user.id, origin = Origin.Tenant, caseRoles = user.roles, isOwner = isOwner)

  def deserialize(json: ValueMap): CaseTeamUser = {
    new CaseTeamUser {
      override val userId: String = json.readString(Fields.userId)
      override val origin: Origin = json.readEnum(Fields.origin, classOf[Origin])
      override val caseRoles: Set[String] = json.readStringList(Fields.caseRoles).toSet
      override val isOwner: Boolean = json.readBoolean(Fields.isOwner)
    }
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
