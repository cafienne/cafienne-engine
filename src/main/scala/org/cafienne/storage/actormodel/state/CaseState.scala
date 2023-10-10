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

package org.cafienne.storage.actormodel.state

import akka.Done
import org.cafienne.cmmn.actorapi.event.plan.{PlanItemCreated, PlanItemTransitioned}
import org.cafienne.cmmn.instance.PlanItemType
import org.cafienne.storage.actormodel.ActorMetadata
import org.cafienne.storage.querydb.CaseStorage

import scala.concurrent.Future

trait CaseState extends QueryDBState {
  override val dbStorage: CaseStorage = new CaseStorage

  override def findCascadingChildren(): Future[Seq[ActorMetadata]] = {
    def taskCreatedFinder(taskType: PlanItemType, finder: String => ActorMetadata): Seq[ActorMetadata] = {
      events
        .filter(_.isInstanceOf[PlanItemCreated])
        .map(_.asInstanceOf[PlanItemCreated])
        .filter(_.getType == taskType)
        .map(_.getPlanItemId).filter(taskMustBeActivated).map(finder).toSeq
    }

    def taskMustBeActivated(taskId: String): Boolean =
      events
        .filter(_.isInstanceOf[PlanItemTransitioned])
        .map(_.asInstanceOf[PlanItemTransitioned])
        .filter(_.getPlanItemId == taskId)
        .exists(_.getCurrentState.isActive)

    Future.successful({
      val cases = taskCreatedFinder(PlanItemType.CaseTask, metadata.caseMember)
      val processes = taskCreatedFinder(PlanItemType.ProcessTask, metadata.processMember)
      //      println(s"Found ${cases.length} cases and ${processes.length} processes: ${cases ++ processes}")
      cases ++ processes
    })
  }

  override def clearQueryData(): Future[Done] = dbStorage.deleteCase(metadata.actorId)
}
