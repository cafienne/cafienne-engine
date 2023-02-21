package org.cafienne.board.state.flow

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.board.actorapi.event.flow.{BoardFlowEvent, FlowActivated, FlowInitiated}
import org.cafienne.board.state.StateElement
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.board.{BoardActor, BoardFields}
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.ValueMap

class FlowState(val board: BoardActor, event: FlowInitiated) extends StateElement with LazyLogging {
  val flowId: String = event.flowId
  private var activationEvent: Option[FlowActivated] = None

  def isActive: Boolean = activationEvent.nonEmpty
  def isCompleted: Boolean = false // Can we know this?

  if (board.recoveryFinished) start()

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and this flow does not have an Activation event, we should try to start the flow again
    if (activationEvent.isEmpty) {
      start()
    }
  }

  def updateState(event: BoardFlowEvent): Unit = event match {
    case event: FlowInitiated => // Handled already
    case event: FlowActivated => activationEvent = Some(event)
    case other => logger.warn(s"Flow $flowId cannot handle event of type ${other.getClass.getName}")
  }


  def start(): Unit = {
    val definition: BoardDefinition = board.state.definition

    // Take the latest & greatest case definition from our board definition
    val caseDefinition = definition.getCaseDefinition()
    // Compose the case team based on the definition
    val caseTeam = definition.team.caseTeam
    // Take the case input from the event
    val caseInput = new ValueMap(BoardFields.BoardMetadata, new ValueMap(Fields.subject, event.subject, BoardDefinition.BOARD_IDENTIFIER, board.getId), BoardFields.Data, event.input)

    val startCase = new StartCase(board.getTenant, event.getUser.asCaseUserIdentity(), flowId, caseDefinition, caseInput, caseTeam, true)
    board.askModel(startCase, (failure: CommandFailure) => {
      logger.warn("Failure while starting flow ", failure.exception)
    }, (success: ModelResponse) => {
      board.addEvent(new FlowActivated(board, event.flowId))
    })
  }
}
