package org.cafienne.system.router

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.infrastructure.Cafienne
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class CafienneGateway(caseSystem: CaseSystem) {
  private val system: ActorSystem = caseSystem.system
  val messageRouterService: ActorRef = system.actorOf(Props.create(classOf[LocalRouter], caseSystem), "LocalRouter")

  val caseRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem), "CaseRouter")
  val processRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem), "ProcessTaskRouter")
  val tenantRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem), "TenantRouter")
  val consentGroupRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem), "ConsentGroupRouter")

  private def getRouter(message: Any): ActorRef = {
    message match {
      case _: CaseCommand =>  caseRouterService
      case _: ProcessCommand => processRouterService
      case _: TenantCommand => tenantRouterService
      case _: ConsentGroupCommand => consentGroupRouterService
      case _ => messageRouterService
    }
  }

  def request(message: ModelCommand): Future[Any] = {
    import akka.pattern.ask
    implicit val timeout: Timeout = Cafienne.config.actor.askTimout

    messageRouterService.ask(message)
  }

  def inform(message: ModelCommand, sender: ActorRef = Actor.noSender): Unit = {
    getRouter(message).tell(message, sender)
  }
}
