package org.cafienne.akka.actor.router

import akka.actor.{ActorRef, Props, Terminated}
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.ModelCommand
import org.cafienne.timerservice.akka.command.TimerServiceCommand

/**
  * In-memory router for akka messages sent in the CaseSystem.
  * Facilitates actor management in a non-clustered actor system.
  */
class LocalRouter extends CaseMessageRouter {
  logger.info("Starting case system in local mode")

  val actors = collection.mutable.Map[String, ActorRef]()

  /**
    * Forward a command to the appropriate ModelActor. Actor will be created if it does not yet exist.
    * @param m
    */
  override def forwardMessage(m: ModelCommand[_]): Unit = {
    val ref: ActorRef = m match {
      case _: TimerServiceCommand => CaseSystem.timerService
      case _ => actors.getOrElseUpdate(m.actorId, createActorRef(m))
    }
    ref.forward(m)
  }

  /**
    * Clean up the actor ref for the actor that has stopped
    * @param t
    * @return
    */
  override def removeActorRef(t: Terminated) = {
    val actorId = t.actor.path.name
    logger.debug("ModelActor["+actorId+"] has been terminated. Removing routing reference")
    if (actors.remove(actorId).isEmpty) {
      logger.warn("Received a Termination message for actor "+actorId+", but it was not registered in the LocalRoutingService. Termination message is ignored")
    }
  }

  /**
    * Creates the ModelActor that can handle the message and starts watching it
    * @param m
    * @return
    */
  private def createActorRef(m: ModelCommand[_]): ActorRef = {
    // Note: we create the ModelActor as a child to our context
    val ref = context.actorOf(Props.create(m.actorClass), m.actorId)
    // Also start watching the lifecycle of the model actor
    context.watch(ref)
    ref
  }
}
