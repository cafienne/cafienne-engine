package org.cafienne.board.state.definition

import org.cafienne.actormodel.identity.{Origin, UserIdentity}
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamUser}

import scala.collection.mutable.ListBuffer

class TeamDefinition(val definition: BoardDefinition) extends DefinitionElement {
  private val users = new ListBuffer[UserIdentity]()

  def updateState(event: BoardDefinitionEvent): Unit = {
    println("Adding case team user " + event.getUser.id)
    users += event.getUser
  }

  def caseTeam: CaseTeam = {
    CaseTeam(users = users.map(u => CaseTeamUser.from(u.id, Origin.IDP)).toSeq, groups = Seq(), tenantRoles = Seq())
  }

  def caseTeamXML(): String = {
    s"""<caseRoles>
       |  <role id="role1" name="Tester" />
       |</caseRoles>""".stripMargin
  }

}
