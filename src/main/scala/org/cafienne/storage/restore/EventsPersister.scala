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

package org.cafienne.storage.restore

import akka.persistence.journal.Tagged
import akka.persistence.{DeleteMessagesSuccess, PersistentActor, RecoveryCompleted}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.archival.{Archive, ModelEventSerializer}
import org.cafienne.storage.restore.command.RestoreArchive
import org.cafienne.storage.restore.event.ChildRestored
import org.cafienne.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala

class EventsPersister(val caseSystem: CaseSystem, val metadata: ActorMetadata) extends PersistentActor with LazyLogging {
  val storageEvents: ListBuffer[StorageEvent] = ListBuffer()
  val events: ListBuffer[ModelEvent] = ListBuffer()

  override def receiveRecover: Receive = {
    case event: StorageEvent =>
      // This one must be cleared at some point
      storageEvents += event
    case event: ModelEvent =>
      // This means we have already restored, right?
      events += event
    case _: RecoveryCompleted => recoveryCompleted()
  }

  def recoveryCompleted(): Unit = {
    log("Recovered, surely hoping we have no events")
    if (hasArchive && hasModelEvents) {

    }

  }

  def log(msg: String): Unit = {
    if (msg.startsWith("\n")) {
      logger.whenDebugEnabled(logger.debug(s"\n${metadata.path}: ${msg.substring(1)}"))
    } else {
      logger.whenDebugEnabled(logger.debug((s"${metadata.path}: $msg")))
    }
  }

  def restoreCompleted(): Unit = {
    context.parent ! ChildRestored(metadata)
    context.stop(self) // Event journal no longer contains our events, we can be deleted
  }

  def clearStorageEvents(): Unit = {
    log(s"Stored ${events.size} actor events; deleting events up to $lastSequenceNr - ${events.size} ==> ${lastSequenceNr - events.size}")
    deleteMessages(lastSequenceNr - events.size)
  }

  def restoreEvents(): Unit = {
    val jsonEvents = archive.events
    val taggedModelEvents = jsonEvents.asScala.toSeq
      .map(_.asMap) // Make it a ValueMap
      .map(ModelEventSerializer.deserializeEvent) // Recover it as ModelEvent instances
      .filter(_.isInstanceOf[ModelEvent])
      .map(_.asInstanceOf[ModelEvent])
      .map(event => Tagged(event, event.tags().asScala.toSet))

    log(s"\nPERSISTING ${taggedModelEvents.size} MODEL EVENTS")

    persistAll(taggedModelEvents)(e => {
      events += e.payload.asInstanceOf[ModelEvent]
      if (e == taggedModelEvents.last) {
        log(s"\nPERSISTED all MODEL EVENTS (last one is ${e.payload.getClass.getSimpleName}), now clearing storage evetns")
        // Events got persisted, now we can remove our progress state events.
        clearStorageEvents()
      }
    })
  }

  def hasModelEvents: Boolean = events.nonEmpty
  def hasArchive: Boolean = storageEvents.exists(_.isInstanceOf[RestoreArchive])
  lazy val archive: Archive = storageEvents.filter(_.isInstanceOf[RestoreArchive]).map(_.asInstanceOf[RestoreArchive]).head.archive

  def validateCommand(command: RestoreArchive): Unit = {
    if (hasModelEvents && hasArchive) {
      // probably already restored ... but not yet cleansed our restore events
      clearStorageEvents()
    } else if (hasModelEvents) {
      // Restored properly, let's tell and stop ourselves immediately
      restoreCompleted()
    } else if (hasArchive) {
      // Apparently not yet restored the archive, let's trigger that process again
      restoreEvents()
    } else {
      // TODO: must we check that we are actually something in "archived" state?
      //if (storageEvents.exists(_.isInstanceOf[ModelActorArchived])) {
      persist(command)(_ => {
        log("\nPERSISTED ARCHIVE, NOW ABOUT TO RESTORE EVENTS")
        storageEvents += command
        restoreEvents()
      })
    }
  }

  override def receiveCommand: Receive = {
    case command: RestoreArchive => validateCommand(command)
    case _: DeleteMessagesSuccess => restoreCompleted()
    case other => logger.warn(s"Received message with unknown type. Ignoring it. Message is of type ${other.getClass.getName}")
  }

  override def persistenceId: String = metadata.actorId
}
