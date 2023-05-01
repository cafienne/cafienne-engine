package org.cafienne.board.state

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.command.CreateBoard
import org.cafienne.board.actorapi.command.flow.StartFlow
import org.cafienne.board.actorapi.command.team.{BoardTeamMemberCommand, RemoveMember, SetMember}
import org.cafienne.board.actorapi.event.definition.BoardDefinitionEvent
import org.cafienne.board.actorapi.event.flow.{BoardFlowEvent, FlowInitiated}
import org.cafienne.board.actorapi.event.team.{BoardManagerAdded, BoardManagerRemoved, BoardTeamEvent}
import org.cafienne.board.actorapi.event.{BoardCreated, BoardEvent, BoardModified}
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.board.state.flow.FlowState
import org.cafienne.board.state.team.BoardTeam
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BoardState(val board: BoardActor, val boardId: String, val optionalJson: Option[ValueMap] = None) extends CafienneJson with LazyLogging {
  val definition: BoardDefinition = optionalJson.fold(new BoardDefinition(this))(BoardDefinition.deserialize(this, _))
  val boardManagers: ListBuffer[String] = new ListBuffer[String]()
  // Deserialize board managers (if available)
  optionalJson.foreach(json => boardManagers.addAll(json.readStringList(Fields.owners)))

  // BoardState holds definition, team,
  val team = new BoardTeam(this)
  val flows = new mutable.HashMap[String, FlowState]()


  def recoveryCompleted(): Unit = {
    // Tell our team we've completed recovery, so that they can start follow up actions when necessary
    team.recoveryCompleted()
    // Tell our flows we've completed recovery, so that they can start follow up actions when necessary
    flows.foreach(_._2.recoveryCompleted())
  }

  def initialize(command: CreateBoard): Unit = {
    board.addEvent(new BoardCreated(definition, command.title, command.form))
    // Be aware: create team is asynchronous, as it creates a Consent Group underneath.
    //  The createTeam is responsible for informing the sender with BoardCreatedResponse.
    team.createTeam(Some(command))
  }

  def startFlow(command: StartFlow): Unit = {
    board.addEvent(new FlowInitiated(board, command.flowId, command.subject, command.data))
    val flow = flows(command.flowId) // FlowInitiated event has updateState that adds the flow to our map of flows.
    // This code is reached when the constructor is called, and hence the FlowInitiated event was triggered.
    //  If in recovery mode, we should not do any further action;
    //  But if in running mode, then let's create the underlying case
    flow.createCase(Some(command))
  }

  def updateBoardManagers(command: BoardTeamMemberCommand): Unit = command match {
    // Check whether we need to change ownership on the team member that is being updated
    case command: SetMember =>
      if (command.member.isOwner && !boardManagers.contains(command.member.userId)) {
        board.addEvent(new BoardManagerAdded(board, command.member.userId))
      } else if (!command.member.isOwner && boardManagers.contains(command.member.userId)) {
        board.addEvent(new BoardManagerRemoved(board, command.member.userId))
      }
    case command: RemoveMember =>
      // If the member was a board manager, remove them from the list
      boardManagers.find(_ == command.memberId).map(_ => board.addEvent(new BoardManagerRemoved(board, command.memberId)))
    case _ => // Ignore other commands
  }

  def updateState(event: BoardEvent): Unit = event match {
    case event: BoardDefinitionEvent => handleDefinitionUpdate(event)
    case event: BoardTeamEvent => team.updateState(event)
    case event: FlowInitiated => flows.getOrElseUpdate(event.flowId, new FlowState(this, event))
    case event: BoardFlowEvent => flows.get(event.flowId).fold({
      logger.error(s"Could not find flow with id ${event.flowId} for event of type ${event.getClass.getName}. This means that FlowInitiated event no longer exists for this flow?!")
    })(_.updateState(event))
    case _: BoardModified => // BoardModified extends ActorModified which updates the state of the actor
    case other => logger.warn(s"Board Definition cannot handle event of type ${other.getClass.getName}")
  }

  private def handleDefinitionUpdate(event: BoardDefinitionEvent): Unit = {
    definition.updateState(event)
    // And now, with the updated definition, we should iterate through all our case instances
    //  and update their case definitions ...
    //  Probably only when recovery is not running ...
    board.addDebugInfo(() => "Updated definition of board " + board.getId + " to:  " + definition.caseDefinition.getDefinitionsDocument.getSource)
  }

  override def toValue: Value[_] = {
    new ValueMap(Fields.id, boardId, Fields.definition, definition.toValue, Fields.owners, boardManagers)
  }
}

object BoardState {
  def deserialize(json: ValueMap): BoardState = {
    val boardId = json.readString(Fields.id)
    new BoardState(null, boardId, Some(json))
  }
}

