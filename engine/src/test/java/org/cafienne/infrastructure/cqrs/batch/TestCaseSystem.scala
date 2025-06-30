package org.cafienne.infrastructure.cqrs.batch

import org.apache.pekko.actor.ActorSystem
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.actormodel.response.ModelResponse
import org.cafienne.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContextExecutor, Future}

class TestCaseSystem(val actorSystem: ActorSystem) {
  val caseSystem: CaseSystem = CaseSystem(actorSystem)
  implicit val dispatcher: ExecutionContextExecutor = actorSystem.dispatcher

  // TODO: This code now directly accesses the Cafienne Gateway; it is intended to be replaced with going through the actual HTTP Routes
  def sendCommand(command: ModelCommand): Future[ModelResponse] = {
    println(s"Requesting gateway with $command")
    caseSystem.engine.request(command).map(response => {
      println("Case System responded with " + response)
      response
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
