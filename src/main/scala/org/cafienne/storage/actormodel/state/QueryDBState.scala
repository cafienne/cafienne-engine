package org.cafienne.storage.actormodel.state

import akka.Done
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.actormodel.event.QueryDataCleared
import org.cafienne.storage.querydb.QueryDBStorage

import scala.concurrent.{ExecutionContext, Future}

trait QueryDBState extends StorageActorState {
  def dbStorage: QueryDBStorage

  implicit def dispatcher: ExecutionContext = dbStorage.dispatcher

  /** Returns true if the query database has been cleaned for the ModelActor
    */
  def queryDataCleared: Boolean = events.exists(_.isInstanceOf[QueryDataCleared])

  /** ModelActor specific implementation to clean up the data generated into the QueryDB based on the
    * events of this specific ModelActor.
    */
  def clearQueryData(): Future[Done] = Future.successful(Done)

  /**
    * ModelActor specific implementation. E.g., a Tenant retrieves it's children from the QueryDB,
    * and a Case can determine it based on the PlanItemCreated events it has.
    *
    * @return
    */
  def findCascadingChildren(): Future[Seq[ActorMetadata]] = Future.successful(Seq())
}
