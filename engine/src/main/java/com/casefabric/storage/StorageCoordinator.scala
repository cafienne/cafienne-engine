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

package com.casefabric.storage

import org.apache.pekko.Done
import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props, Terminated}
import org.apache.pekko.persistence.query.{EventEnvelope, Offset}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import com.typesafe.scalalogging.LazyLogging
import com.casefabric.infrastructure.CaseFabric
import com.casefabric.infrastructure.cqrs.ReadJournalProvider
import com.casefabric.storage.actormodel.event.StorageRequestReceived
import com.casefabric.storage.actormodel.message.{StorageActionCompleted, StorageActionStarted, StorageCommand, StorageEvent}
import com.casefabric.storage.actormodel.{ActorMetadata, StorageActorSupervisor}
import com.casefabric.storage.archival.command.ArchiveActorData
import com.casefabric.storage.archival.event.ArchivalStarted
import com.casefabric.storage.deletion.command.RemoveActorData
import com.casefabric.storage.deletion.event.RemovalStarted
import com.casefabric.storage.restore.command.RestoreActorData
import com.casefabric.storage.restore.event.RestoreStarted
import com.casefabric.system.CaseSystem
import com.casefabric.system.health.HealthMonitor

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class StorageCoordinator(val caseSystem: CaseSystem)
  extends Actor
    with StorageActorSupervisor
    with ReadJournalProvider
    with LazyLogging {

  override val system: ActorSystem = caseSystem.system
  implicit val ec: ExecutionContext = caseSystem.system.dispatcher

  if (CaseFabric.config.engine.storage.recoveryDisabled) {
    logger.warn("WARNING: Storage Coordination Service does not recover any existing unfinished storage processes; set 'engine.storage-service.auto-start = true' to enable recovery ")
  } else {
    logger.warn("Launching Storage Coordination Service")
    start()
  }

  private def getActor(command: StorageCommand): ActorRef = getActorRef(s"root_${command.metadata.actorId}", Props(command.RootStorageActorClass, caseSystem, command.metadata))

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
      case EventEnvelope(_, _, _, event: StorageActionStarted) =>
        if (event.metadata.isRoot) {
          def restart(commandMaker: ActorMetadata => StorageCommand): Unit = {
            val command = commandMaker(event.metadata)
            logger.info(s"Recovering storage process '${command.getClass.getSimpleName}' on actor ${event.metadata}")
            getActor(command).tell(command, self)
          }

          event match {
            case _: RemovalStarted => restart(RemoveActorData)
            case _: ArchivalStarted => restart(ArchiveActorData)
            case _: RestoreStarted => restart(RestoreActorData)
            case other => logger.warn(s"Cannot recover a storage process, because of unrecognized initiation event of type ${other.getClass.getName}")
          }
        }
      case EventEnvelope(_, _, _, _: StorageEvent) => // Other storage events can be safely ignored.
      case other => logger.error(s"Encountered unexpected storage tag matching event of type ${other.getClass.getName}")
    }
    Future.successful(Done)
  }

  override def receive: Receive = {
    case command: StorageCommand =>
      logger.whenDebugEnabled(logger.debug(s"Received $command"))
      val originalSender = sender()
      // Tell CaseFabric Engine to remove this model actor from memory,
      //  and then forward the command to the appropriate StorageActor, on behalf of the sender()
      terminateModelActor(command.metadata, {
        logger.whenDebugEnabled(logger.debug(s"Actor ${command.metadata.actorId} terminated, triggering follow up: $command"))
        getActor(command).tell(command, originalSender)
      })
    case event: StorageRequestReceived =>
      logger.whenDebugEnabled(logger.debug(s"Started storage request on ${event.metadata}"))
    case event: StorageActionCompleted =>
      // Nothing needs to be done, as the actor will stop itself and below we handle the resulting Termination message.
      logger.whenDebugEnabled(logger.debug(s"Completed storage action for ${event.metadata}"))
    case t: Terminated => removeActorRef(t)
    case other => logger.warn(s"StorageCoordinator received an unknown message of type ${other.getClass.getName}")
  }
}
