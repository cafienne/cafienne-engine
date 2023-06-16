/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.storage

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.command.TerminateModelActor
import org.cafienne.actormodel.response.ActorTerminated
import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.message.{StorageActionInitiated, StorageCommand, StorageEvent}
import org.cafienne.storage.archival.command.ArchiveActorData
import org.cafienne.storage.archival.event.ArchivalInitiated
import org.cafienne.storage.deletion.command.RemoveActorData
import org.cafienne.storage.deletion.event.{RemovalCompleted, RemovalInitiated}
import org.cafienne.storage.restore.command.RestoreActorData
import org.cafienne.storage.restore.event.RestoreInitiated
import org.cafienne.system.CaseSystem
import org.cafienne.system.health.HealthMonitor

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class StorageCoordinator(val caseSystem: CaseSystem) extends Actor with LazyLogging with ReadJournalProvider {
  override val system: ActorSystem = caseSystem.system
  implicit val ec: ExecutionContext = caseSystem.system.dispatcher

  private val refs: mutable.Map[String, ActorRef] = new mutable.HashMap[String, ActorRef]()

  if (Cafienne.config.engine.storage.recoveryDisabled) {
    logger.warn("WARNING: Storage Coordination Service does not recover any existing unfinished storage processes; set 'engine.storage-service.auto-start = true' to enable recovery ")
  } else {
    logger.warn("Launching Storage Coordination Service")
    start()
  }

  private def getActorRef(command: StorageCommand): ActorRef = {
    refs.getOrElseUpdate(command.metadata.actorId, {
      // Note: we create the ModelActor as a child to our context
      val ref = context.actorOf(Props(command.actorClass, caseSystem, command.metadata), command.metadata.actorId)
      // Also start watching the lifecycle of the model actor
      context.watch(ref)
      ref
    })
  }

  def start(): Unit = {
    runStream() onComplete {
      case Success(_) =>
        logger.info("Completed re-activating Storage Deletion Actors")
      case Failure(ex) =>
        logger.error(getClass.getSimpleName + " bumped into an issue that it cannot recover from.", ex)
        HealthMonitor.storageService.hasFailed(ex)
    }
  }

  def runStream(): Future[Done] = {
    implicit val mat: Materializer = Materializer(context)

    journal().currentEventsByTag(StorageEvent.TAG, Offset.noOffset).mapAsync(1)(consumeModelEvent).runWith(Sink.ignore)
  }

  def consumeModelEvent(envelope: EventEnvelope): Future[Done] = {
    envelope match {
      // Trigger deletion process on actors that still have a StorageEvent (the first one is always RemovalInitiated).
      //  But only trigger it on top level removals, as they will themselves instantiate their children that have not yet been deleted.
      case EventEnvelope(_, _, _, event: StorageActionInitiated) =>
        if (event.metadata.isRoot) {
          def restart(commandMaker: ActorMetadata => StorageCommand): Unit = {
            val command = commandMaker(event.metadata)
            logger.info(s"Recovering storage process '${command.getClass.getSimpleName}' on actor ${event.metadata}")
            getActorRef(command).tell(command, self)
          }

          event match {
            case _: RemovalInitiated => restart(RemoveActorData)
            case _: ArchivalInitiated => restart(ArchiveActorData)
            case _: RestoreInitiated => restart(RestoreActorData)
            case other => logger.warn(s"Cannot recover a storage process, because of unrecognized initiation event of type ${other.getClass.getName}")
          }
        }
      case EventEnvelope(_, _, _, _: StorageEvent) => // Other storage events can be safely ignored.
      case other => logger.error(s"Encountered unexpected storage tag matching event of type ${other.getClass.getName}")
    }
    Future.successful(Done)
  }

  private val followups = mutable.Map[String, (StorageCommand, ActorRef)]()
  private def registerFollowup(command: StorageCommand, originalSender: ActorRef): Unit = {
    followups.put(command.metadata.actorId, (command, originalSender))
  }

  private def handleFollowup(message: ActorTerminated): Unit = {
    followups.remove(message.actorId).foreach(request => {
      val command = request._1
      val originalSender = request._2
      logger.whenDebugEnabled(logger.debug(s"Actor ${message.actorId} terminated, triggering follow up: $command"))
      getActorRef(command).tell(command, originalSender)
    })
  }

  override def receive: Receive = {
    case command: StorageCommand =>
      logger.whenDebugEnabled(logger.debug(s"Received $command"))
      registerFollowup(command, sender())
      // Tell Cafienne Engine to remove this model actor from memory
      caseSystem.gateway.inform(new TerminateModelActor(command.metadata.user, command.metadata.actorId), self)
    case message: ActorTerminated => handleFollowup(message)
    case event: RemovalCompleted =>
      // Nothing needs to be done, as the actor will stop itself and below we handle the resulting Termination message.
      logger.whenDebugEnabled(logger.debug(s"Completed removal for ${event.metadata}"))
    case t: Terminated =>
      val actorId = t.actor.path.name
      if (refs.remove(actorId).isEmpty) {
        logger.warn("Received a Termination message for actor " + actorId + ", but it was not registered in the LocalRoutingService. Termination message is ignored")
      }
      logger.whenDebugEnabled(logger.debug(s"Actor $actorId is removed from memory"))
  }
}
