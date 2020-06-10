package org.cafienne.cmmn.akka.command.team

import org.cafienne.cmmn.akka.command.team
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.casefile.{Value, ValueList, ValueMap}

import scala.collection.JavaConverters._

case class CaseTeam(members: Seq[CaseTeamMember] = Seq()) {

  /**
    * Validates whether the case team setup matches the case definition
    *
    * @param caseDefinition Definition to validate against
    */
  def validate(caseDefinition: CaseDefinition): Unit = {
    members.map(m => m.validateRolesExist(caseDefinition))
  }

  def owners(): Seq[CaseTeamMember] = {
    members.filter(member => member.isOwner.fold(false)(isOwner => isOwner))
  }

  def getMembers() = members.asJava

  override def toString: String = toValue.toString

  def toValue(): ValueList = {
    Value.convert(members.map(member => member.toValue)).asInstanceOf[ValueList]
  }
}

object CaseTeam {

  import scala.collection.JavaConverters._

  def apply() = new CaseTeam(Seq())

  def apply(members: java.util.Collection[CaseTeamMember]) = new CaseTeam(members.asScala.toSeq)

  def apply(member: CaseTeamMember) = new CaseTeam(Seq(member))

  def deserialize(memberList: ValueList) = {
    val teamMembers = memberList.getValue.asScala.asInstanceOf[Seq[ValueMap]].map(json => CaseTeamMember.deserialize(json))
    team.CaseTeam(teamMembers)
  }
}