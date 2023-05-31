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

import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.json.{ValueList, ValueMap}
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.state.QueryDBState
import org.cafienne.storage.archival.event._
import org.cafienne.storage.archival.event.cmmn.ModelActorArchived
import org.cafienne.storage.archival.response.ArchivalCompleted
import org.cafienne.storage.archival.{ActorDataArchiver, Archive, ModelEventSerializer}

trait ArchivalState extends QueryDBState {
  override val actor: ActorDataArchiver

  override def handleStorageEvent(event: StorageEvent): Unit = {
    event match {
      case event: ArchivalStarted =>
        if (event.actorId == actorId) {
          printLogMessage(s"Starting archival for ${event.metadata}")
          continueStorageProcess()
        }
      case _: ChildrenArchivalInitiated => triggerChildArchivalProcess()
      case _: QueryDataArchived =>
        printLogMessage(s"QueryDB has been archived")
        checkArchivingDone()
      case event: ChildArchived =>
        actor.confirmChildArchived(event)
        checkArchivingDone()
      case _: ArchivalCompleted => // One of the children is done.
      case event: ArchiveCreated => actor.afterArchiveCreated(event)
      case _: ArchiveExported => actor.afterArchiveExported()
      case _: ModelActorArchived => actor.completeStorageProcess()
      case _ => reportUnknownEvent(event)
    }
  }

  def isCreated: Boolean = events.exists(_.isInstanceOf[ArchiveCreated])

  def isExported: Boolean = events.exists(_.isInstanceOf[ArchiveExported])

  def isCleared: Boolean = events.exists(_.isInstanceOf[ModelActorArchived])

  /** The archival process is idempotent (i.e., it can be triggered multiple times without ado).
    * It is typically triggered when recovery is done or after the first incoming ArchiveActorData command is received.
    * It triggers both child archival and cleaning query data.
    */
  override def continueStorageProcess(): Unit = {
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
    if (!queryDataCleared) {
      printLogMessage("Archiving query data")
      clearQueryData().map(_ => actor.self ! QueryDataArchived(metadata))
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

  /** Determine if all data is archived from children and also from QueryDB.
    * If so, then invoke the final deletion of all actor events, including the StorageEvents that have been created during the deletion process
    */
  def checkArchivingDone(): Unit = {
    printLogMessage(
      s"Running completion check: [queryDataCleared=$queryDataCleared; childrenMetadataAvailable=$childrenMetadataAvailable; children archived=${children.size - pendingChildArchivals.size}, pending=${pendingChildArchivals.size}]"
    )
    if (childArchivesAvailable && queryDataCleared) {
      if (!isCreated) {
        actor.createArchive()
      }
    }
  }

  def childArchivesAvailable: Boolean = childrenMetadataAvailable && pendingChildArchivals.isEmpty

  /**
    * Returns the archives of our children
    *
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
    //  Note: we're setting sequence_number of the event, starting with 1 instead of 0;
    //  This makes it sort of compliant with how it is done by Akka in event journal. Helps relating events properly.
    ModelEventSerializer.serializeEventToJson(element._1, element._2 + 1)
  }

  /**
    * Final event to give an indication that the ModelActor has been archived
    * Up to the ModelActor specific type of state to give the event the proper name.
    *
    * @return
    */
  def createModelActorStorageEvent: ModelActorArchived
}
