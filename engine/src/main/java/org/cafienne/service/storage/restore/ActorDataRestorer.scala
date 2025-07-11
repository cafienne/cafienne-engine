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

package org.cafienne.service.storage.restore

import org.apache.pekko.persistence.journal.Tagged
import org.apache.pekko.persistence.{DeleteMessagesSuccess, RecoveryCompleted}
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.service.storage.actormodel.message.StorageEvent
import org.cafienne.service.storage.actormodel.{ActorMetadata, BaseStorageActor}
import org.cafienne.service.storage.archival.{Archive, ModelEventSerializer}
import org.cafienne.service.storage.restore.command.RestoreArchive
import org.cafienne.service.storage.restore.event.RestoreCompleted
import org.cafienne.system.CaseSystem

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.CollectionHasAsScala

class ActorDataRestorer(val caseSystem: CaseSystem, val metadata: ActorMetadata) extends BaseStorageActor with LazyLogging {
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
    if (hasArchive && hasModelEvents) {
      // Then ... why are we recovering?
    }
  }

  def afterStorageProcessCompleted(): Unit = {
    context.parent ! RestoreCompleted(metadata)
    context.stop(self) // Event journal no longer contains our events, we can be deleted
  }

  private var startedClearing = false;

  def clearStorageEvents(): Unit = {
    if (! startedClearing) {
      printLogMessage(s"Stored ${events.size} actor events; deleting events up to $lastSequenceNr - ${events.size} ==> ${lastSequenceNr - events.size}")
      deleteMessages(lastSequenceNr - events.size)
      startedClearing = true // Avoid clearing twice, as that results in dead letter messages (DeleteMessagesSuccess comes then multiple times)
    }
  }

  def restoreEvents(): Unit = {
    val jsonEvents = archive.events
    val taggedModelEvents = jsonEvents.asScala.toSeq
      .map(_.asMap) // Make it a ValueMap
      .map(ModelEventSerializer.deserializeEvent) // Recover it as ModelEvent instances
      .filter(_.isInstanceOf[ModelEvent])
      .map(_.asInstanceOf[ModelEvent])
      .map(event => Tagged(event, event.tags().asScala.toSet))

    printLogMessage(s"\nPERSISTING ${taggedModelEvents.size} MODEL EVENTS")

    persistAll(taggedModelEvents)(e => {
      events += e.payload.asInstanceOf[ModelEvent]
      if (e == taggedModelEvents.last) {
        printLogMessage(s"\nPERSISTED all MODEL EVENTS (last one is ${e.payload.getClass.getSimpleName}), now clearing storage evetns")
        // Events got persisted, now we can remove our progress state events.
        clearStorageEvents()
      }
    })
  }

  def hasModelEvents: Boolean = events.nonEmpty
  def hasArchive: Boolean = storageEvents.exists(_.isInstanceOf[RestoreArchive])
  lazy val archive: Archive = storageEvents.filter(_.isInstanceOf[RestoreArchive]).map(_.asInstanceOf[RestoreArchive]).head.archive

  def startStorageProcess(command: RestoreArchive): Unit = {
    if (hasModelEvents && hasArchive) {
      // probably already restored ... but not yet cleansed our restore events
      clearStorageEvents()
    } else if (hasModelEvents) {
      // Restored properly, let's tell and stop ourselves immediately
      afterStorageProcessCompleted()
    } else if (hasArchive) {
      // Apparently not yet restored the archive, let's trigger that process again
      restoreEvents()
    } else {
      // TODO: must we check that we are actually something in "archived" state?
      //if (storageEvents.exists(_.isInstanceOf[ModelActorArchived])) {
      persist(command)(_ => {
        printLogMessage("\nPERSISTED ARCHIVE, NOW ABOUT TO RESTORE EVENTS")
        storageEvents += command
        restoreEvents()
      })
    }
  }

  override def receiveCommand: Receive = {
    case command: RestoreArchive => startStorageProcess(command)
    case _: DeleteMessagesSuccess => afterStorageProcessCompleted()
    case other => reportUnknownMessage(other)
  }

  override def persistenceId: String = metadata.actorId
}
