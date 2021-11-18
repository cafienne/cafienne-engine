package org.cafienne.cmmn.actorapi.command.team

import org.cafienne.cmmn.definition.CaseDefinition
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

  def deserialize(memberList: ValueList): CaseTeam = {
    val teamMembers = memberList.getValue.asScala.toSeq.asInstanceOf[Seq[ValueMap]].map(json => CaseTeamMember.deserialize(json))
    CaseTeam(teamMembers)
  }
}