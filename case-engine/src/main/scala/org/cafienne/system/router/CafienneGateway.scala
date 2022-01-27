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
  private val caseService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "cases")
  private val processTaskService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "process-tasks")
  private val tenantService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "tenants")
  private val consentGroupService = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "consent-groups")
  private val defaultRouterService: ActorRef = system.actorOf(Props.create(classOf[LocalRouter], caseSystem, actors), "default-router")

  private def getRouter(message: Any): ActorRef = {
    message match {
      case _: CaseCommand =>  caseService
      case _: ProcessCommand => processTaskService
      case _: TenantCommand => tenantService
      case _: ConsentGroupCommand => consentGroupService
      case _ => defaultRouterService
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
