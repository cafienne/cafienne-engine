package org.cafienne.system.router

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.cmmn.actorapi.command.CaseCommand
import org.cafienne.consentgroup.actorapi.command.ConsentGroupCommand
import org.cafienne.infrastructure.Cafienne
import org.cafienne.processtask.actorapi.command.ProcessCommand
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.command.TenantCommand

import scala.concurrent.Future

class CafienneGateway(caseSystem: CaseSystem) {
  private val system: ActorSystem = caseSystem.system
  private val actors = collection.concurrent.TrieMap[String, ActorRef]()
  private val messageRouterService: ActorRef = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "LocalRouter")
  private val caseRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "CaseRouter")
  private val processRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "ProcessTaskRouter")
  private val tenantRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "TenantRouter")
  private val consentGroupRouterService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "ConsentGroupRouter")

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

    getRouter(message).ask(message)
  }

  def inform(message: ModelCommand, sender: ActorRef = Actor.noSender): Unit = {
    getRouter(message).tell(message, sender)
  }
}
