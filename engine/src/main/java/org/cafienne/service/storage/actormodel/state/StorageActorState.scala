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

package org.cafienne.service.storage.actormodel.state

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorRef
import org.cafienne.actormodel.event.{ModelEvent, ModelEventCollection}
import org.cafienne.service.storage.actormodel.command.ClearTimerData
import org.cafienne.service.storage.actormodel.event.{ChildrenReceived, QueryDataCleared, TimerDataCleared}
import org.cafienne.service.storage.actormodel.message.{StorageActionStarted, StorageEvent}
import org.cafienne.service.storage.actormodel.{ActorMetadata, BaseStorageActor}
import org.cafienne.service.storage.archival.event.ArchivalStarted
import org.cafienne.service.storage.querydb.QueryDBStorage

trait StorageActorState extends ModelEventCollection with LazyLogging {
  def dbStorage: QueryDBStorage

  val actor: BaseStorageActor
  val rootStorageActor: ActorRef = actor.context.parent

  val metadata: ActorMetadata = actor.metadata
  val actorId: String = metadata.actorId

  def originalModelActorEvents: Seq[ModelEvent] = events.filterNot(_.isInstanceOf[StorageEvent]).toSeq

  def printLogMessage(msg: String): Unit = actor.printLogMessage(msg)

  def reportUnknownEvent(event: StorageEvent): Unit = {
    logger.error(s"${actor.getClass.getSimpleName}[$metadata]: Cannot handle event of type ${event.getClass.getName} from ${event.metadata}")
  }

  def actualModelActorType: String = events
    .filter(_.isBootstrapMessage)
    .map(_.asBootstrapMessage())
    .map(_.actorClass.getName)
    .headOption // Take the actor class of the bootstrap message found, or else just give a message with the event types that are found.
    .getOrElse(s"Bootstrap message is missing; found ${events.length} events of types: [${events.map(_.getClass.getName).toSet.mkString(",")}]")

  def createStorageStartedEvent: StorageActionStarted

  def hasStartEvent: Boolean = events.exists(_.isInstanceOf[StorageActionStarted])

  def storageStartedEvent: StorageActionStarted = getEvent(classOf[StorageActionStarted])

  def startStorageProcess(): Unit = {
    val children = findCascadingChildren()
    printLogMessage(s"Found ${children.length} children: ${children.mkString("\n--- ", s"\n--- ", "")}")
    informOwner(ArchivalStarted(metadata, children))
  }

  /**
   * ModelActor specific implementation. E.g., a Tenant retrieves it's children from the QueryDB,
   * and a Case can determine it based on the PlanItemCreated events it has.
   *
   * @return
   */
  def findCascadingChildren(): Seq[ActorMetadata] = Seq()

  /** Returns true if the RootStorageActor knows about our children
   */
  def parentReceivedChildrenInformation: Boolean = events.exists(_.isInstanceOf[ChildrenReceived])

  /**
   * ModelActor specific implementation to clean up the timers registered in the TimerService
   */
  def clearTimerData(): Unit = {
    actor.caseSystem.service.informTimerService(ClearTimerData(metadata), actor.self)
  }

  /**
   * Returns true if the TimerService has removed any running timers initiated by the ModelActor
   */
  def timerDataCleared: Boolean = events.exists(_.isInstanceOf[TimerDataCleared])

  /**
   * Returns true if the query database has been cleaned for the ModelActor
   */
  def queryDataCleared: Boolean = events.exists(_.isInstanceOf[QueryDataCleared])

  /**
   * ModelActor specific implementation to clean up the data generated into the QueryDB based on the
   * events of this specific ModelActor.
   */
  def clearQueryData(): Unit

  /**
    * Continues the storage process.
    * Note, this method must be idempotent as it can be invoked multiple times.
    */
  def continueStorageProcess(): Unit

  def checkStorageProcessCompletion(): Unit

  def addEvent(event: ModelEvent): Unit = {
    events += event
    // If an event is added during recovery, we should not invoke follow up actions, since first full recovery has to complete.
    // If, on the other hand, recovery finished, we probably get a new storage event,
    //  so then determine and invoke followup logic immediately.
    if (actor.recoveryFinished && event.isInstanceOf[StorageEvent]) {
      handleStorageEvent(event.asInstanceOf[StorageEvent])
    }
  }

  def handleStorageEvent(event: StorageEvent): Unit

  def informOwner(msg: Any): Unit = {
    rootStorageActor ! msg
  }

  /**
    * Triggers the removal process upon recovery completion. But only if the RemovalInitiated event is found.
    */
  def handleRecoveryCompletion(): Unit = {
    printLogMessage(s"Recovery completed with ${events.size} events")
    if (hasStartEvent) {
      println(s"$metadata: Skipping continuation after recovery")
      printLogMessage("Triggering storage process upon recovery")
//      continueStorageProcess()
    }
  }

  /**
    * Check if we have recovered events of the expected type of ModelActor
    * (to e.g. avoid deleting a case if we're expecting to delete a tenant)
    */
  def hasExpectedEvents: Boolean = events.exists(event => metadata.actorType.actorEventClass.isAssignableFrom(event.getClass))
}
