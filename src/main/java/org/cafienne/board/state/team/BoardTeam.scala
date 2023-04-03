package org.cafienne.board.state.team

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.{BoardUser, ConsentGroupUser, Origin}
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
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable}

class BoardTeam(val definition: BoardDefinition, val users: ListBuffer[BoardUser] = new ListBuffer[BoardUser]()) extends DefinitionElement with CafienneJson with LazyLogging {
  val teamId: String = board.getId + BoardTeam.EXTENSION
  val roles: mutable.Set[String] = new mutable.HashSet[String]()
  private var boardCreatedEvent: Option[BoardCreated] = None
  private var teamCreatedEvent: Option[BoardTeamCreated] = None

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and the board's consent group is not yet created, we should try to do that
    if (boardCreatedEvent.nonEmpty && teamCreatedEvent.isEmpty) {
      createTeam(None, users.head)
    }
  }

  def createTeam(command: Option[CreateBoard] = None, user: BoardUser): Unit = {
    // Trigger the creation of the underlying consent group.
    //  Note: this method is called when recovery finished and we do not yet have a consent group
    //  and it is also called from the board creation command.

    val user: ConsentGroupUser = ConsentGroupUser(users.head.id, teamId, board.getTenant)
    val group = ConsentGroup(teamId, board.getTenant, members = Seq(ConsentGroupMember(user.id, Seq(), true)))
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
      sender ! new BoardTeamResponse(command, response.lastModifiedContent())
    })
  }

  def handle(command: BoardTeamMemberCommand): Unit = command match {
    case command: SetMember => delegate(command, new SetConsentGroupMember(_, command.member), "set team member")
    case command: RemoveMember => delegate(command, new RemoveConsentGroupMember(_, command.memberId), "remove team member")
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
    users += BoardUser(event.getUser.id, board.getId)
  }

  def updateState(event: BoardTeamEvent): Unit = event match {
    case event: BoardTeamCreated => teamCreatedEvent = Some(event)
    case event: RoleDefinitionAdded => roles.add(event.roleName)
    case event: RoleDefinitionRemoved => roles.remove(event.roleName)
    case other => logger.warn(s"Team Definition cannot handle event of type ${other.getClass.getName}")
  }

  def members(): Array[BoardUser] = users.toArray

  //  def roles(): Set[String] = users.flatMap(_.)

  def caseTeam: CaseTeam = {
    // TODO: keep track of a list of BoardManagers, and make them all case owner?
    val groups = Seq(new CaseTeamGroup(teamId, mappings = roles.map(r => GroupRoleMapping(r, caseRoles = immutable.Set(r))).toSeq))
    CaseTeam(users = users.map(u => CaseTeamUser.from(userId = u.id, origin = Origin.IDP)).toSeq, groups = groups, tenantRoles = Seq())
  }

  def caseTeamXML(): String = {
    s"""<caseRoles>
       |  ${roles.map(role => s"""<role id="${role}" name="${role}" />""")}
       |</caseRoles>""".stripMargin
  }

  override def toValue: Value[_] = new ValueList(users.map(_.toValue).toArray)
}

object BoardTeam {
  val EXTENSION = "-team"
  def deserialize(json: ValueList): Seq[BoardUser] = json.getValue.toArray(Array[ValueMap]()).toSeq.map(BoardUser.deserialize)
}
