package org.cafienne.board.state.flow

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.exception.InvalidCommandException
import org.cafienne.actormodel.identity.CaseUserIdentity
import org.cafienne.actormodel.response.{CommandFailure, ModelResponse}
import org.cafienne.board.BoardFields
import org.cafienne.board.actorapi.command.BoardCommand
import org.cafienne.board.actorapi.command.definition.BoardDefinitionCommand
import org.cafienne.board.actorapi.command.flow._
import org.cafienne.board.actorapi.event.flow.{BoardFlowEvent, FlowActivated, FlowCanceled, FlowInitiated}
import org.cafienne.board.actorapi.response.FlowStartedResponse
import org.cafienne.board.actorapi.response.runtime.FlowResponse
import org.cafienne.board.state.definition.BoardDefinition
import org.cafienne.board.state.{BoardState, StateElement}
import org.cafienne.cmmn.actorapi.command.migration.MigrateDefinition
import org.cafienne.cmmn.actorapi.command.plan.MakeCaseTransition
import org.cafienne.cmmn.actorapi.command.plan.eventlistener.RaiseEvent
import org.cafienne.cmmn.actorapi.command.{CaseCommand, StartCase}
import org.cafienne.cmmn.instance.Transition
import org.cafienne.humantask.actorapi.command.{ClaimTask, CompleteHumanTask, SaveTaskOutput}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.ValueMap

import scala.concurrent.Future

class FlowState(val state: BoardState, event: FlowInitiated) extends StateElement with LazyLogging {
  val flowId: String = event.flowId
  private var activationEvent: Option[FlowActivated] = None

  def isActive: Boolean = activationEvent.nonEmpty
  def isCompleted: Boolean = false // Can we know this?

  def recoveryCompleted(): Unit = {
    // If board completed recovery, and this flow does not have an Activation event, we should try to start the flow again
    if (activationEvent.isEmpty) {
      createCase()
    }
  }

  def updateState(event: BoardFlowEvent): Unit = event match {
    case _: FlowInitiated => // Handled already
    case event: FlowActivated => activationEvent = Some(event)
    case event: FlowCanceled => board.state.flows.remove(event.flowId)
    case other => logger.warn(s"Flow $flowId cannot handle event of type ${other.getClass.getName}")
  }

  def createCase(command: Option[StartFlow] = None): Unit = {
    val definition: BoardDefinition = board.state.definition

    // Take the latest & greatest case definition from our board definition
    val caseDefinition = definition.caseDefinition
    // Compose the case team based on the definition
    val caseTeam = state.team.caseTeam
    // Take the case input from the event
    val caseInput = new ValueMap(BoardFields.BoardMetadata, new ValueMap(Fields.subject, event.subject, BoardDefinition.BOARD_IDENTIFIER, board.getId), BoardFields.Data, event.input)

    val startCase = new StartCase(board.getTenant, event.getUser.asCaseUserIdentity(), flowId, caseDefinition, caseInput, caseTeam, true)
    val sender = command.map(_.sender)
    // TODO: somehow make delegate method more generic to handle this
    board.askModel(startCase, (failure: CommandFailure) => {
      logger.warn("Failure while starting flow ", failure.exception)
      sender.map(_ ! failure)
    }, (success: ModelResponse) => {
      board.addEvent(new FlowActivated(board, event.flowId))
      sender.map(_ ! new FlowStartedResponse(command.get, flowId, success.lastModifiedContent()))
    })
  }

  private def delegate(command: BoardCommand, caseCommand: CaseCommand, event: Option[BoardFlowEvent] = None): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val sender = board.sender()
    askModelActor(caseCommand).map {
      case failure: CommandFailure =>
        sender ! failure
      case success: ModelResponse =>
        event.foreach(addEvent)
        sender ! new FlowResponse(command, success.lastModifiedContent())
    }
  }

  private def delegateTaskCommand(command: FlowTaskCommand, commandCreator: (CaseUserIdentity, String, String) => CaseCommand): Unit = {
    val caseCommand = commandCreator(command.getUser.asCaseUserIdentity(), command.flowId, command.taskId)
    if (caseCommand != null) {
      delegate(command, caseCommand)
    }
  }

  def handle(command: BoardFlowCommand): Unit = command match {
    case command: ClaimFlowTask => delegateTaskCommand(command, (u, f, t) => new ClaimTask(u, f, t))
    case command: SaveFlowTaskOutput => delegateTaskCommand(command, (u, f, t) => new SaveTaskOutput(u, f, t, command.output()))
    case command: CompleteFlowTask => delegateTaskCommand(command, (u, f, t) => new CompleteHumanTask(u, f, t, command.output()))
    case command: CancelFlowTask =>
      // We can only cancel a task if it is not in the first column
      val cancelEventName = state.definition.columns.find(_.getTitle == command.taskId).map(_.eventName).filter(_.nonEmpty)
      cancelEventName.fold(throw new InvalidCommandException(s"Cannot handle cancel task '${command.taskId}' as there is no previous task that would become active"))(cancelEventName => {
        delegateTaskCommand(command, (u, f, _) => new RaiseEvent(u, f, cancelEventName))
      })
    case command: CancelFlow => delegate(command, new MakeCaseTransition(command.getUser.asCaseUserIdentity(), command.flowId, Transition.Terminate), Some(new FlowCanceled(board, command.flowId)))
    case other => throw new InvalidCommandException(s"Cannot handle commands of type ${other.getClass.getName}")
  }

  def migrateDefinition(command: BoardDefinitionCommand): Future[ModelResponse] = {
    askModelActor(new MigrateDefinition(command.getUser.asCaseUserIdentity(), flowId, state.definition.caseDefinition, state.team.caseTeam))
  }
}
