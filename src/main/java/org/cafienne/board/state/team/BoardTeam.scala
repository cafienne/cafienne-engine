package org.cafienne.board.state.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.{ConsentGroupUser, Origin}
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.board.actorapi.command.CreateBoard
import org.cafienne.board.actorapi.command.definition.role.{AddBoardRole, RemoveBoardRole}
import org.cafienne.board.actorapi.command.team._
import org.cafienne.board.actorapi.event.BoardCreated
import org.cafienne.board.actorapi.event.definition.{RoleDefinitionAdded, RoleDefinitionRemoved}
import org.cafienne.board.actorapi.event.team._
import org.cafienne.board.actorapi.response.{BoardCreatedResponse, BoardTeamResponse}
import org.cafienne.board.state.definition.{BoardDefinition, DefinitionElement}
import org.cafienne.cmmn.actorapi.command.team.{CaseTeam, CaseTeamGroup, CaseTeamUser, GroupRoleMapping}
import org.cafienne.consentgroup.actorapi.command.{ConsentGroupCommand, CreateConsentGroup, RemoveConsentGroupMember, SetConsentGroupMember}
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}
import org.cafienne.json.{CafienneJson, Value, ValueList}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class BoardTeam(val definition: BoardDefinition) extends DefinitionElement with CafienneJson with LazyLogging {
  val teamId: String = board.getId + BoardTeam.EXTENSION
  val roles: mutable.Set[String] = new mutable.HashSet[String]()
  val boardManagers: ListBuffer[String] = new ListBuffer[String]()
  private var boardCreatedEvent: Option[BoardCreated] = None
  private var teamCreatedEvent: Option[BoardTeamCreated] = None

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and the board's consent group is not yet created, we should try to do that
    if (boardCreatedEvent.nonEmpty && teamCreatedEvent.isEmpty) {
      createTeam(None, boardManagers.head)
    }
  }

  def createTeam(command: Option[CreateBoard] = None, user: String): Unit = {
    // Trigger the creation of the underlying consent group.
    //  Note: this method is called when recovery finished and we do not yet have a consent group
    //  and it is also called from the board creation command.

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

  private def delegate(command: BoardTeamMemberCommand, commandCreator: ConsentGroupUser => ConsentGroupCommand, event: Option[BoardTeamEvent], errorMsg: String): Unit = {
    val sender = board.sender()
    val owner = new ConsentGroupUser(command.getUser.id, teamId, board.getTenant)
    board.askModel(commandCreator(owner), (failure: CommandFailure) => {
      logger.warn(s"Could not $errorMsg", failure.exception)
      sender ! failure // TODO: wrap it in a board failure or so
    }, (response: ModelResponse) => {
      event.foreach(board.addEvent)
      sender ! new BoardTeamResponse(command, response.lastModifiedContent())
    })
  }

  def setMember(command: SetMember): Unit = {
    val event = {
      if (command.member.isOwner && ! boardManagers.contains(command.member.userId)) {
        Some(new BoardManagerAdded(board, command.member.userId))
      } else if (! command.member.isOwner && boardManagers.contains(command.member.userId)) {
        Some(new BoardManagerRemoved(board, command.member.userId))
      } else {
        None
      }
    }
    delegate(command, new SetConsentGroupMember(_, command.member), event, "set team member")
  }

  def removeMember(command: RemoveMember): Unit = {
    val event: Option[BoardManagerRemoved] = boardManagers.find(_ == command.memberId).map(_ => new BoardManagerRemoved(board, command.memberId))
    delegate(command, new RemoveConsentGroupMember(_, command.memberId), event, "remove team member")
  }

  def handle(command: BoardTeamMemberCommand): Unit = command match {
    case command: SetMember => setMember(command)
    case command: RemoveMember => removeMember(command)
    case command: AddBoardRole => upsertTeamRole(command.roleName)
    case command: RemoveBoardRole => if (roles.contains(command.roleName)) board.addEvent(new RoleDefinitionRemoved(board, command.roleName))
    case other => throw new InvalidCommandException(s"Cannot handle commands of type ${other.getClass.getName}")
  }

  def upsertTeamRole(roleName: String): Unit = {
    // Don't add blank roles
    if (!roleName.isBlank && !roles.contains(roleName)) board.addEvent(new RoleDefinitionAdded(board, roleName))
  }

  def updateState(event: BoardCreated): Unit = {
    boardCreatedEvent = Some(event)
    boardManagers += event.getUser.id
  }

  def updateState(event: BoardTeamEvent): Unit = event match {
    case event: BoardTeamCreated => teamCreatedEvent = Some(event)
    case event: RoleDefinitionAdded => roles.add(event.roleName)
    case event: RoleDefinitionRemoved => roles.remove(event.roleName)
    case event: BoardManagerAdded => boardManagers += event.userId
    case event: BoardManagerRemoved => boardManagers -= event.userId
    case other => logger.warn(s"Team Definition cannot handle event of type ${other.getClass.getName}")
  }

  def caseTeam: CaseTeam = {
    // TODO: keep track of a list of BoardManagers, and make them all case owner?
    val mappings = roles.map(r => GroupRoleMapping(r, caseRoles = immutable.Set(r))).toSeq ++ Seq(GroupRoleMapping(groupRole = "", caseRoles = Set("")))
    val groups = Seq(new CaseTeamGroup(teamId, mappings = mappings))
    CaseTeam(users = boardManagers.map(u => CaseTeamUser.from(userId = u, origin = Origin.IDP, isOwner = true)).toSeq, groups = groups, tenantRoles = Seq())
  }

  def caseTeamXML(): String = {
    s"""<caseRoles>
       |  ${roles.map(role => s"""<role id="${role}" name="${role}" />""")}
       |</caseRoles>""".stripMargin
  }

  override def toValue: Value[_] = new ValueList(boardManagers.toArray)
}

object BoardTeam {
  val EXTENSION = "-team"
  def deserialize(json: ValueList): Seq[String] = json.getValue.toArray(Array[String]()).toSeq
}
