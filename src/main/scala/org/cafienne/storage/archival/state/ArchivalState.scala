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

package org.cafienne.storage.archival.state

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.infrastructure.serialization.{CafienneSerializer, Fields}
import org.cafienne.json.{ValueList, ValueMap}
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.{ActorMetadata, StorageActorState}
import org.cafienne.storage.archival.event._
import org.cafienne.storage.archival.{ActorDataArchiver, Archive}
import org.cafienne.storage.querydb.QueryDBStorage

import scala.concurrent.{ExecutionContext, Future}

trait ArchivalState extends StorageActorState with LazyLogging {
  override val actor: ActorDataArchiver

  def dbStorage: QueryDBStorage

  implicit def dispatcher: ExecutionContext = dbStorage.dispatcher

  override def handleStorageEvent(event: StorageEvent): Unit = {
    event match {
      case event: ArchivalInitiated =>
        printLogMessage(s"Starting archival for ${event.metadata}")
        triggerArchivalProcess()
        actor.sender() ! event
      case _: ChildrenArchivalInitiated => triggerChildArchivalProcess()
      case _: QueryDataArchived =>
        printLogMessage(s"QueryDB has been archived")
        checkArchivingDone()
      case event: ChildArchived =>
        actor.confirmChildArchived(event)
        checkArchivingDone()
      case event: ArchiveCreated => actor.afterArchiveCreated(event)
      case _: ArchiveExported => actor.afterArchiveExported()
      case _: ModelActorArchived => actor.afterModelActorEventStored()
      case event =>
        logger.error(s"Cannot handle event of type ${event.getClass.getName} in ${actor.getClass.getSimpleName}[${event.actorId}]")
    }
  }


  def isCreated: Boolean = events.exists(_.isInstanceOf[ArchiveCreated])

  def isExported: Boolean = events.exists(_.isInstanceOf[ArchiveExported])

  def isCleared: Boolean = events.exists(_.isInstanceOf[ModelActorArchived])

  /** Triggers the archival process upon recovery completion. But only if the ArchivalInitiated event is found.
   */
  def handleRecoveryCompletion(): Unit = {
    printLogMessage(s"Recovery completed with ${events.size} events")
    if (events.exists(_.isInstanceOf[ArchivalInitiated])) {
      printLogMessage("Launching recovered archival process")
      triggerArchivalProcess()
    }
  }

  /** ModelActor specific implementation to clean up the data generated into the QueryDB based on the
   * events of this specific ModelActor.
   */
  def archiveQueryData(): Future[Done]

  /** The archival process is idempotent (i.e., it can be triggered multiple times without ado).
   * It is typically triggered when recovery is done or after the first incoming ArchiveActorData command is received.
   * It triggers both child archival and cleaning query data.
   */
  def triggerArchivalProcess(): Unit = {
    triggerChildArchivalProcess()
    triggerQueryDBCleanupProcess()
  }

  /** Child archival process consists of 2 steps:
   * 1. Determine what the children are, this is done by the ModelActor specific state (e.g., a Tenant needs QueryDB info,
   * cases can do it with PlanItemCreated events).
   * When the info is found, it is sent to self as a command, such that we can store the event from it
   * 2. When the event with the children metadata is available, we can trigger each of those children
   * to archive themselves. When the child is fully cleaned (including it's own children), it reports back to us.
   */
  def triggerChildArchivalProcess(): Unit = {
    if (!childrenMetadataAvailable) {
      // Use the db storage connection pool to provide threads.
      //  The reason is that only the query db relevant futures will need to do an actual logic
      findCascadingChildren().map { children =>
        printLogMessage(s"Found ${children.length} children to be archived: ${children.mkString("\n--- ", s"\n--- ", "")}")
        actor.self ! ChildrenArchivalInitiated(actor.metadata, children)
      }
    } else {
      if (pendingChildArchivals.nonEmpty) {
        printLogMessage(s"Found ${pendingChildArchivals.size} out of ${children.size} children with pending archival")
        pendingChildArchivals.foreach(actor.archiveChildActorData)
      } else {
        printLogMessage(s"No children found that have pending archival")
        checkArchivingDone()
      }
    }
  }

  /** Let the ModelActor specific state clean up the QueryDB unless it is already done
   */
  def triggerQueryDBCleanupProcess(): Unit = {
    if (!queryDataArchived) {
      printLogMessage("Archiving query data")
      archiveQueryData().map(_ => actor.self ! QueryDataArchived(metadata))
    }
    checkArchivingDone()
  }

  /** Determine if we have an event with the metadata of all our children
   */
  def childrenMetadataAvailable: Boolean = events.exists(_.isInstanceOf[ChildrenArchivalInitiated])

  /** Retrieve metadata of all our children. Only makes sense if the children metadata is available...
   */
  def children: Seq[ActorMetadata] = eventsOfType(classOf[ChildrenArchivalInitiated]).flatMap(_.members)

  /** Determine which children have not yet reported back that their data is archived
   */
  def pendingChildArchivals: Seq[ActorMetadata] = children.filterNot(isAlreadyArchived)

  /** Returns true if there is a ArchivalCompleted event for this child metadata
   */
  def isAlreadyArchived(child: ActorMetadata): Boolean = eventsOfType(classOf[ChildArchived]).exists(_.metadata == child)

  /** Returns true if the query database has been cleaned for the ModelActor
   */
  def queryDataArchived: Boolean = events.exists(_.isInstanceOf[QueryDataArchived])

  /** Determine if all data is archived from children and also from QueryDB.
   * If so, then invoke the final deletion of all actor events, including the StorageEvents that have been created during the deletion process
   */
  def checkArchivingDone(): Unit = {
    printLogMessage(
      s"Running completion check: [queryDataCleared=$queryDataArchived; childrenMetadataAvailable=$childrenMetadataAvailable; children archived=${children.size - pendingChildArchivals.size}, pending=${pendingChildArchivals.size}]"
    )
    if (childArchivesAvailable && queryDataArchived) {
      if (! isCreated) {
        actor.storeEvent(createArchiveEvent)
      }
    }
  }

  def childArchivesAvailable: Boolean = childrenMetadataAvailable && pendingChildArchivals.isEmpty

  /**
   * Returns the archives of our children
   * @return
   */
  def childArchives: Seq[Archive] = eventsOfType(classOf[ChildArchived]).map(_.archive)

  def createArchiveEvent: ArchiveCreated = {
    printLogMessage(s"Starting final step to delete ourselves from event journal")

    // Create the archive
    val archive = createArchive
    ArchiveCreated(metadata, archive)
  }

  def archive: ArchiveCreated = {
    // Note: invoking this method when isCreated returns false ... throws something on None
    events.find(_.isInstanceOf[ArchiveCreated]).get.asInstanceOf[ArchiveCreated]
  }

  def createArchive: Archive = {
    val list: ValueList = new ValueList()
    // Convert the events to JSON.
    originalModelActorEvents.zipWithIndex.map(serializeEventToJson).foreach(list.add)
    Archive(metadata, list, children = childArchives)
  }

  def serializeEventToJson(element: (ModelEvent, Int)): ValueMap = {
    val event = element._1
    //  Note: we're setting sequence_number of the event, starting with 1 instead of 0;
    //  This makes it sort of compliant with how it is done by Akka in event journal. Helps relating events properly.
    val sequenceNr = element._2 + 1

    new ValueMap(Fields.sequenceNr, sequenceNr, Fields.manifest, CafienneSerializer.getManifestString(event), Fields.content, event.rawJson())
  }

  /**
   * Final event to give an indication that the ModelActor has been archived
   * Up to the ModelActor specific type of state to give the event the proper name.
   * @return
   */
  def createModelActorEvent: ModelActorArchived
}
