package org.cafienne.board.state.team

import org.cafienne.actormodel.identity.{BoardUser, Origin}
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent
import org.cafienne.board.state.definition.{BoardDefinition, DefinitionElement}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamUser}
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}

import scala.collection.mutable.ListBuffer

class BoardTeam(val definition: BoardDefinition, val users: ListBuffer[BoardUser] = new ListBuffer[BoardUser]()) extends DefinitionElement with CafienneJson {
  def updateState(event: BoardDefinitionEvent): Unit = {
    println("Adding case team user " + event.getUser.id)
    users += BoardUser(event.getUser.id, board.getId)
  }

  def members(): Array[BoardUser] = users.toArray

  def caseTeam: CaseTeam = {
    CaseTeam(users = users.map(u => CaseTeamUser.from(u.id, Origin.IDP)).toSeq, groups = Seq(), tenantRoles = Seq())
  }

  def caseTeamXML(): String = {
    s"""<caseRoles>
       |  <role id="role1" name="Tester" />
       |</caseRoles>""".stripMargin
  }

  override def toValue: Value[_] = new ValueList(users.map(_.toValue).toArray)
}

object BoardTeam {
  def deserialize(json: ValueList): Seq[BoardUser] = json.getValue.toArray(Array[ValueMap]()).toSeq.map(BoardUser.deserialize)
}
