package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.cmmn.actorapi.command.team
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.team.CaseTeamError
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}

import scala.jdk.CollectionConverters._

case class CaseTeam(members: Seq[CaseTeamMember] = Seq(), caseRoles: Seq[String] = Seq(), unassignedRoles: Seq[String] = Seq()) extends CafienneJson {

  /**
    * Validates whether the case team setup matches the case definition
    *
    * @param caseDefinition Definition to validate against
    */
  def validate(caseDefinition: CaseDefinition): Unit = {
    members.foreach(m => m.validateRolesExist(caseDefinition))

    // Go through all defined case roles
    // and check that new team does not have conflicting interests.
    caseDefinition.getCaseTeamModel.getCaseRoles.forEach(role => {
      val roleName = role.getName
      if (role.isSingleton) {
        // Only one user can have a singleton role assigned
        if (members.count(p => p.caseRoles.contains(roleName)) > 1) {
          throw new CaseTeamError(s"Role '$roleName' cannot be assigned to more than one team member")
        }
      }
      // Users can not have multiple mutexing roles assigned to them
      val mutexRoles = role.getMutexRoles
      mutexRoles.forEach(mutexedRole => {
        val mutexRole = mutexedRole.getName
        if (members.exists(member => member.getCaseRoles.contains(mutexRole) && member.getCaseRoles.contains(roleName))) {
          throw new CaseTeamError(s"A team member cannot have both roles '$roleName' and '$mutexRole'")
        }
      })
    })
  }

  def owners(): Seq[CaseTeamMember] = {
    members.filter(member => member.isOwner.fold(false)(isOwner => isOwner))
  }

  def getMembers() = members.asJava

  override def toValue: Value[_] = {
    new ValueMap("caseRoles", caseRoles, "members", members, "unassignedRoles", unassignedRoles)
  }
}

object CaseTeam {

  def apply() = new CaseTeam(Seq())

  def apply(members: java.util.Collection[CaseTeamMember]) = new CaseTeam(members.asScala.toSeq)

  def apply(member: CaseTeamMember) = new CaseTeam(Seq(member))

  def deserialize(memberList: ValueList) = {
    val teamMembers = memberList.getValue.asScala.toSeq.asInstanceOf[Seq[ValueMap]].map(json => CaseTeamMember.deserialize(json))
    team.CaseTeam(teamMembers)
  }
}