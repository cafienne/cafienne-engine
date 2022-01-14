package org.cafienne.system.router

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.util.Timeout
import org.cafienne.system.CaseSystem

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class CafienneGateway(caseSystem: CaseSystem) {
  private val system: ActorSystem = caseSystem.system
  val messageRouterService: ActorRef = system.actorOf(Props.create(classOf[LocalRouter], caseSystem), "LocalRouter")

  def request(message: Any): Future[Any] = {
    import akka.pattern.ask
    implicit val timeout: Timeout = Timeout(10.seconds)

    messageRouterService.ask(message)
  }

  def inform(msg: Any, sender: ActorRef = Actor.noSender): Unit = {
    messageRouterService.tell(msg, sender)
  }
}
