package org.cafienne.infrastructure.cqrs.batch

import akka.actor.ActorSystem
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.infrastructure.Cafienne
import org.cafienne.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContextExecutor, Future}

class TestCaseSystem(val actorSystem: ActorSystem = ActorSystem("Test-Case-System", Cafienne.config.systemConfig)) {
  val caseSystem = new CaseSystem(actorSystem)
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher

  // TODO: This code now directly accesses the Cafieene Gateway; it is intended to be replaced with going through the actual Akka HTTP Routes
  def sendCommand(command: ModelCommand): Future[ModelResponse] = {
    println(s"Requesting gateway with $command")
    caseSystem.gateway.request(command).map(response => {
      println("Case System responded with " + response)
      response.asInstanceOf[ModelResponse] // Hard cast. If it's not a ModelResponse, then anyway something is wrong
    })
  }

  def runCommands(commands: Seq[ModelCommand]): Future[Seq[ModelResponse]] = {
    val responses: ListBuffer[ModelResponse] = ListBuffer()
    val sendCommandChain = {
      var chainOfSendCommandFutures: Future[Any] = Future {}
      for (command <- commands) chainOfSendCommandFutures = chainOfSendCommandFutures.flatMap( _ => sendCommand(command).map(responses += _))
      chainOfSendCommandFutures
    }
    // Return the response list
    sendCommandChain.map(_ => responses.toSeq)
  }
}
