package org.cafienne.board.state.definition

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{BoardUser, ConsentGroupUser, Origin}
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.board.actorapi.event.BoardCreated
import org.cafienne.board.actorapi.event.team.{BoardTeamCreated, BoardTeamCreationFailed, BoardTeamEvent}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamUser}
import org.cafienne.consentgroup.actorapi.command.CreateConsentGroup
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}

import scala.collection.mutable.ListBuffer

class TeamDefinition(val definition: BoardDefinition, val users: ListBuffer[BoardUser] = new ListBuffer[BoardUser]()) extends DefinitionElement with CafienneJson with LazyLogging {
  val teamId = board.getId + TeamDefinition.EXTENSION
  private var boardCreatedEvent: Option[BoardCreated] = None
  private var teamCreatedEvent: Option[BoardTeamCreated] = None

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and the board's consent group is not yet created, we should try to do that
    if (boardCreatedEvent.nonEmpty && teamCreatedEvent.isEmpty) {
      createTeam(users.head)
    }
  }

  def createTeam(user: BoardUser): Unit = {
    // Trigger the creation of the underlying consent group.
    //  Note: this method is called when recovery finished and we do not yet have a consent group
    //  and it is also called from the board creation command.

    val user: ConsentGroupUser = ConsentGroupUser(users.head.id, teamId, board.getTenant)
    val group = ConsentGroup(teamId, board.getTenant, members = Seq(ConsentGroupMember(user.id, Seq(), true)))
    val createGroupCommand = new CreateConsentGroup(user, group)

    board.askModel(createGroupCommand, (failure: CommandFailure) => {
      logger.warn("Failure while creating board team as consent group ", failure.exception)
      board.addEvent(new BoardTeamCreationFailed(board, failure))
    }, (response: ModelResponse) => {
      val teamLastModified = response.lastModifiedContent()
      board.addEvent(new BoardTeamCreated(board, teamId))
    })
  }

  def updateState(event: BoardCreated): Unit = {
    boardCreatedEvent = Some(event)
    users += BoardUser(event.getUser.id, board.getId)
  }

  def updateState(event: BoardTeamEvent): Unit = event match {
    case event: BoardTeamCreated => teamCreatedEvent = Some(event)
    case other => logger.warn(s"Team Definition cannot handle event of type ${other.getClass.getName}")
  }

  def members(): Array[BoardUser] = users.toArray

  //  def roles(): Set[String] = users.flatMap(_.)

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
  val EXTENSION = "-team"
  def deserialize(json: ValueList): Seq[BoardUser] = json.getValue.toArray(Array[ValueMap]()).toSeq.map(BoardUser.deserialize)
}
