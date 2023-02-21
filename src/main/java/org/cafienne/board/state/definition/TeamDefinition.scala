package org.cafienne.board.state.definition

import org.cafienne.actormodel.identity.{BoardUser, Origin}
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamUser}
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}

import scala.collection.mutable.ListBuffer

class TeamDefinition(val definition: BoardDefinition, val users: ListBuffer[BoardUser] = new ListBuffer[BoardUser]()) extends DefinitionElement with CafienneJson {
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

object TeamDefinition {
  def deserialize(json: ValueList): Seq[BoardUser] = json.getValue.toArray(Array[ValueMap]()).toSeq.map(BoardUser.deserialize)
}
