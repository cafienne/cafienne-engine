package org.cafienne.board.state.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.{ConsentGroupUser, Origin}
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.board.actorapi.command.CreateBoard
import org.cafienne.board.actorapi.command.team._
import org.cafienne.board.actorapi.event.BoardCreated
import org.cafienne.board.actorapi.event.team._
import org.cafienne.board.actorapi.response.{BoardCreatedResponse, BoardTeamResponse}
import org.cafienne.board.state.{BoardState, StateElement}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamGroup, CaseTeamUser, GroupRoleMapping}
import org.cafienne.consentgroup.actorapi.command.{ConsentGroupCommand, CreateConsentGroup, RemoveConsentGroupMember, SetConsentGroupMember}
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}

import scala.collection.immutable
import scala.collection.mutable.ListBuffer

class BoardTeam(val state: BoardState) extends StateElement with LazyLogging {
  val teamId: String = board.getId + BoardTeam.EXTENSION
  def boardManagers: ListBuffer[String] = state.boardManagers
  private var boardCreatedEvent: Option[BoardCreated] = None
  private var teamCreatedEvent: Option[BoardTeamCreated] = None

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and the board's consent group is not yet created, we should try to do that
    if (boardCreatedEvent.nonEmpty && teamCreatedEvent.isEmpty) {
      createTeam(None)
    }
  }

  def createTeam(command: Option[CreateBoard] = None): Unit = {
    // Trigger the creation of the underlying consent group.
    //  Note: this method is called when recovery finished and we do not yet have a consent group
    //  and it is also called from the board creation command.

    val boardManagers = state.boardManagers

    val user: ConsentGroupUser = ConsentGroupUser(boardManagers.head, teamId, board.getTenant)
    val group = ConsentGroup(teamId, board.getTenant, members = boardManagers.map(ConsentGroupMember(_, isOwner = true)).toSeq)
    val createGroupCommand = new CreateConsentGroup(user, group)
    val sender = command.map(_ => board.sender())

    board.askModel(createGroupCommand, (failure: CommandFailure) => {
      logger.warn("Failure while creating board team as consent group ", failure.exception)
      board.addEvent(new BoardTeamCreationFailed(board, failure))
    }, (response: ModelResponse) => {
      board.addEvent(new BoardTeamCreated(board, teamId))
      sender.map(_ ! new BoardCreatedResponse(command.get, board.getId, response.lastModifiedContent))
    })
  }

  private def delegate(command: BoardTeamMemberCommand, commandCreator: ConsentGroupUser => ConsentGroupCommand, errorMsg: String): Unit = {
    val sender = board.sender()
    val owner = new ConsentGroupUser(command.getUser.id, teamId, board.getTenant)
    board.askModel(commandCreator(owner), (failure: CommandFailure) => {
      logger.warn(s"Could not $errorMsg", failure.exception)
      sender ! failure // TODO: wrap it in a board failure or so
    }, (response: ModelResponse) => {
      state.updateBoardManagers(command)
      sender ! new BoardTeamResponse(command, response.lastModifiedContent())
    })
  }

  def handle(command: BoardTeamMemberCommand): Unit = command match {
    case command: SetMember => delegate(command, new SetConsentGroupMember(_, command.member), "set team member")
    case command: RemoveMember => delegate(command, new RemoveConsentGroupMember(_, command.memberId), "remove team member")
    case other => throw new InvalidCommandException(s"Cannot handle commands of type ${other.getClass.getName}")
  }

  def updateState(event: BoardCreated): Unit = {
    boardCreatedEvent = Some(event)
    boardManagers += event.getUser.id
  }

  def updateState(event: BoardTeamEvent): Unit = event match {
    case event: BoardTeamCreated => teamCreatedEvent = Some(event)
    case event: BoardManagerAdded => boardManagers += event.userId
    case event: BoardManagerRemoved => boardManagers -= event.userId
    case other => logger.warn(s"Team Definition cannot handle event of type ${other.getClass.getName}")
  }

  def caseTeam: CaseTeam = {
    // TODO: keep track of a list of BoardManagers, and make them all case owner?
    val mappings = state.definition.roles.map(r => GroupRoleMapping(r, caseRoles = immutable.Set(r))).toSeq ++ Seq(GroupRoleMapping(groupRole = "", caseRoles = Set("")))
    val groups = Seq(new CaseTeamGroup(teamId, mappings = mappings))
    CaseTeam(users = boardManagers.map(u => CaseTeamUser.from(userId = u, origin = Origin.IDP, isOwner = true)).toSeq, groups = groups, tenantRoles = Seq())
  }
}

object BoardTeam {
  val EXTENSION = "-team"
}
