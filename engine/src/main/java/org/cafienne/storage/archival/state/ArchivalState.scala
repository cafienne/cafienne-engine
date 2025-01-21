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
import org.cafienne.storage.actormodel.event.{ChildrenReceived, QueryDataCleared}
import org.cafienne.storage.actormodel.message.StorageEvent
import org.cafienne.storage.actormodel.state.QueryDBState
import org.cafienne.storage.archival.event._
import org.cafienne.storage.archival.event.cmmn.ModelActorArchived
import org.cafienne.storage.archival.{ActorDataArchiver, Archive, ModelEventSerializer}

trait ArchivalState extends QueryDBState {
  override val actor: ActorDataArchiver

  override def handleStorageEvent(event: StorageEvent): Unit = {
    event match {
      case event: ArchivalStarted =>
        printLogMessage(s"Starting archival for ${event.metadata}")
        continueStorageProcess()
      case _: ChildrenReceived =>
        printLogMessage(s"Stored children information")
        continueStorageProcess()
      case _: QueryDataCleared =>
        printLogMessage(s"QueryDB has been archived")
        continueStorageProcess()
      case event: ArchiveCreated => actor.afterArchiveCreated(event)
      case _: ArchiveReceived => actor.afterArchiveExported()
      case _: ModelActorArchived => actor.completeStorageProcess()
      case _ => reportUnknownEvent(event)
    }
  }

  def isCreated: Boolean = events.exists(_.isInstanceOf[ArchiveCreated])

  def parentReceivedArchive: Boolean = events.exists(_.isInstanceOf[ArchiveReceived])

  def isCleared: Boolean = events.exists(_.isInstanceOf[ModelActorArchived])

  override def startStorageProcess(): Unit = {
    val children = findCascadingChildren()
    printLogMessage(s"Found ${children.length} children: ${children.mkString("\n--- ", s"\n--- ", "")}")
    informOwner(ArchivalStarted(metadata, children))
  }

  /** The archival process is idempotent (i.e., it can be triggered multiple times without ado).
    * It is typically triggered when recovery is done or after the first incoming ArchiveActorData command is received.
    * It triggers both child archival and cleaning query data.
    */
  override def continueStorageProcess(): Unit = {
    if (!parentReceivedChildrenInformation) {
      printLogMessage("Initiating storage process")
      startStorageProcess()
    } else {
      if (!queryDataCleared) {
        printLogMessage("Archiving query data")
        // Note: instead of removing the info from the QueryDB we could also
        // set the state in the QueryDB to Archived. Requests to getCase could then return "Case is archived" or so.
        //  However, then we still would also have to keep authorization information, as that is required
        //  per individual case instance. But then ... the case would not really be archived?!
        //  Therefore, it is up to the invoker of the archiving logic to handle such a situation.
        clearQueryData()
        actor.self ! QueryDataArchived(metadata)
      }
    }
    checkArchivingDone()
  }

  /** Determine if all data is archived from children and also from QueryDB.
    * If so, then invoke the final deletion of all actor events, including the StorageEvents that have been created during the deletion process
    */
  def checkArchivingDone(): Unit = {
    printLogMessage(s"Running completion check: [queryDataCleared=$queryDataCleared;]")
    if (queryDataCleared) {
      if (!isCreated) {
        actor.createArchive()
      }
    }
  }

  def createArchiveEvent: ArchiveCreated = {
    printLogMessage(s"Starting final step to delete ourselves from event journal")

    // Create the archive
    def serializeEventToJson(element: (ModelEvent, Int)): ValueMap = {
      //  Note: we're setting sequence_number of the event, starting with 1 instead of 0;
      //  This makes it sort of compliant with how it is done in the underlying event journal. Helps relating events properly.
      ModelEventSerializer.serializeEventToJson(element._1, element._2 + 1)
    }

    val list: ValueList = new ValueList()
    // Convert the events to JSON.
    originalModelActorEvents.zipWithIndex.map(serializeEventToJson).foreach(list.add)
    val archive = Archive(metadata, list, children = Seq())
    ArchiveCreated(metadata, archive)
  }

  def archive: ArchiveCreated = {
    // Note: invoking this method when isCreated returns false ... throws something on None
    events.find(_.isInstanceOf[ArchiveCreated]).get.asInstanceOf[ArchiveCreated]
  }

  /**
    * Final event to give an indication that the ModelActor has been archived
    * Up to the ModelActor specific type of state to give the event the proper name.
    *
    * @return
    */
  def createModelActorStorageEvent: ModelActorArchived
}
