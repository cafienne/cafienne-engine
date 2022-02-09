package org.cafienne.system.router

import akka.actor.{ActorRef, Props, Terminated}
import org.cafienne.actormodel.command.ModelCommand
import org.cafienne.system.CaseSystem

/**
  * In-memory router for akka messages sent in the CaseSystem.
  * Facilitates actor management in a non-clustered actor system.
  */
class LocalRouter(caseSystem: CaseSystem, actors: collection.concurrent.TrieMap[String, ActorRef]) extends CaseMessageRouter {
  logger.info("Starting case system in local mode")

  /**
    * Forward a command to the appropriate ModelActor. Actor will be created if it does not yet exist.
    *
    * @param m
    */
  override def forwardMessage(m: ModelCommand): Unit = {
    actors.getOrElseUpdate(m.actorId, createActorRef(m)).forward(m)
  }

  /**
    * Creates the ModelActor that can handle the message and starts watching it
    *
    * @param m
    * @return
    */
  private def createActorRef(m: ModelCommand): ActorRef = {
    // Note: we create the ModelActor as a child to our context
    val ref = context.actorOf(Props.create(m.actorClass, caseSystem), m.actorId)
    // Also start watching the lifecycle of the model actor
    context.watch(ref)
    ref
  }

  override def terminateActor(actorId: String): Unit = {
    actors.get(actorId).foreach(actor => context.stop(actor))
  }

  /**
    * Clean up the actor ref for the actor that has stopped
    *
    * @param t
    * @return
    */
  override def removeActorRef(t: Terminated): Unit = {
    val actorId = t.actor.path.name
    logger.whenDebugEnabled(logger.debug("ModelActor[" + actorId + "] has been terminated. Removing routing reference"))
    if (actors.remove(actorId).isEmpty) {
      logger.warn("Received a Termination message for actor " + actorId + ", but it was not registered in the LocalRoutingService. Termination message is ignored")
    }
  }
}
