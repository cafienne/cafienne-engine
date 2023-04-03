package org.cafienne.board.state

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.CaseUserIdentity
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.board.actorapi.command.flow._
import org.cafienne.board.actorapi.event.flow.{BoardFlowEvent, FlowActivated, FlowInitiated}
import org.cafienne.board.actorapi.response.FlowStartedResponse
import org.cafienne.board.actorapi.response.runtime.FlowResponse
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.board.{BoardActor, BoardFields}
import org.cafienne.cmmn.actorapi.command.{CaseCommand, StartCase}
import org.cafienne.humantask.actorapi.command.{ClaimTask, CompleteHumanTask, SaveTaskOutput}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.ValueMap

class FlowState(val board: BoardActor, event: FlowInitiated) extends StateElement with LazyLogging {
  val flowId = event.flowId
  private var activationEvent: Option[FlowActivated] = None

  def isActive = activationEvent.nonEmpty

  def isCompleted = false // Can we know this?

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and this flow does not have an Activation event, we should try to start the flow again
    if (activationEvent.isEmpty) {
      createCase()
    }
  }

  def updateState(event: BoardFlowEvent): Unit = event match {
    case event: FlowInitiated => // Handled already
    case event: FlowActivated => activationEvent = Some(event)
    case other => logger.warn(s"Flow $flowId cannot handle event of type ${other.getClass.getName}")
  }

  def createCase(command: Option[StartFlow] = None): Unit = {
    val definition: BoardDefinition = board.state.definition

    // Take the latest & greatest case definition from our board definition
    val caseDefinition = definition.caseDefinition
    // Compose the case team based on the definition
    val caseTeam = definition.team.caseTeam
    // Take the case input from the event
    val caseInput = new ValueMap(BoardFields.BoardMetadata, new ValueMap(Fields.subject, event.subject, BoardDefinition.BOARD_IDENTIFIER, board.getId), BoardFields.Data, event.input)

    val startCase = new StartCase(board.getTenant, event.getUser.asCaseUserIdentity(), flowId, caseDefinition, caseInput, caseTeam, true)
    val sender = command.map(_ => board.sender())
    // TODO: somehow make delegate method more generic to handle this
    board.askModel(startCase, (failure: CommandFailure) => {
      logger.warn("Failure while starting flow ", failure.exception)
      sender.map(_ ! failure)
    }, (success: ModelResponse) => {
      board.addEvent(new FlowActivated(board, event.flowId))
      sender.map(_ ! new FlowStartedResponse(command.get, flowId, success.lastModifiedContent()))
    })
  }

  private def delegate(command: FlowTaskCommand, commandCreator: (CaseUserIdentity, String, String) => CaseCommand, errorMsg: String): Unit = {
    val sender = board.sender()
    board.askModel(commandCreator(command.getUser.asCaseUserIdentity(), command.flowId, command.taskId), (failure: CommandFailure) => {
      logger.warn(s"Could not $errorMsg", failure.exception)
      sender ! failure // TODO: wrap it in a board failure or so
    }, (success: ModelResponse) => {
      sender ! new FlowResponse(command, success.lastModifiedContent())
    })
  }

  def handle(command: FlowTaskCommand): Unit = command match {
    case command: ClaimFlowTask => delegate(command, (u, f, t) => new ClaimTask(u, f, t), "claim task")
    case command: SaveFlowTaskOutput => delegate(command, (u, f, t) => new SaveTaskOutput(u, f, t, command.output()), "save task output")
    case command: CompleteFlowTask => delegate(command, (u, f, t) => new CompleteHumanTask(u, f, t, command.output()), "complete task")
    case other => throw new InvalidCommandException(s"Cannot handle commands of type ${other.getClass.getName}")
  }
}
