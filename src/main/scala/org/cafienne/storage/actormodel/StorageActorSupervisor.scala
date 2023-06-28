package org.cafienne.storage.actormodel

import akka.actor.{Actor, ActorRef, Props, Terminated}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.TerminateModelActor
import org.cafienne.system.CaseSystem

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

trait StorageActorSupervisor extends Actor with LazyLogging {
  val caseSystem: CaseSystem
  protected val childActors: mutable.Map[String, ActorRef] = new mutable.HashMap[String, ActorRef]()

  def getActorRef(metadata: ActorMetadata, props: Props): ActorRef = getActorRef(metadata.actorId, props)

  def getActorRef(actorId: String, props: Props): ActorRef = {
    childActors.getOrElseUpdate(actorId, {
      // Note: we create the ModelActor as a child to our context
      val ref = context.actorOf(props, actorId)
      // Also start watching the lifecycle of the model actor
      context.watch(ref)
      ref
    })
  }

  def removeActorRef(message: Terminated): Unit = {
    // TODO: can we also find the actor ref from the values with message.actor instead of using the name?
    val actorId = message.actor.path.name
    if (childActors.remove(actorId).isEmpty) {
      logger.warn("Received a Termination message for actor " + actorId + ", but it was not registered in the LocalRoutingService. Termination message is ignored")
    }
    logger.whenDebugEnabled(logger.debug(s"Actor $actorId is removed from memory"))
  }

  /**
    * Tell Cafienne Gateway to remove the actual ModelActor with the same actor id from memory.
    * Upon successful termination, the followup action will be triggered.
    */
  def terminateModelActor(metadata: ActorMetadata, followUpAction: => Unit = {}): Unit = {
    caseSystem.gateway.request(TerminateModelActor(metadata.actorId)).onComplete{
      case Success(value) => followUpAction
      case Failure(exception) => logger.warn(s"Failure upon termination of model actor $metadata", exception)
    }
  }
}
