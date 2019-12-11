package org.cafienne.infrastructure.cqrs

import akka.pattern.ask
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.ModelCommand
import org.cafienne.akka.actor.command.response.ModelResponse
import org.cafienne.service.Main

import scala.concurrent.Future

trait CommandSender {
  private val caseRegion = CaseSystem.caseMessageRouter()

  implicit val executionContext = scala.concurrent.ExecutionContext.global
  implicit val timeout = Main.caseSystemTimeout

  def sendCommandToCase(command: ModelCommand[_]): Future[ModelResponse] = {
    val f = caseRegion ? command
    f.map(f => f.asInstanceOf[ModelResponse])
  }
}
