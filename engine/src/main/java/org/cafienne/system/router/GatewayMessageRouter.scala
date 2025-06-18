package org.cafienne.system.router

import org.apache.pekko.actor.{Actor, ActorRef}

import scala.concurrent.Future

trait GatewayMessageRouter {

  def request(message: Any): Future[Any]

  def inform(message: Any, sender: ActorRef = Actor.noSender): Unit

  def createTimerService: ActorRef

  //NOTE @Thijs: the storagecoordinator messaging is handled in the gateway impl, so doesn't need
  // exposure like this right now.
  def createStorageCoordinator: ActorRef
}
