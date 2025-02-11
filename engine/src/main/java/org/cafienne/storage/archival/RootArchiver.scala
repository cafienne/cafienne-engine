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

package org.cafienne.storage.archival

import org.apache.pekko.actor.Actor
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.storage.actormodel.{ActorMetadata, RootStorageActor}
import org.cafienne.storage.archival.event.ArchivalRequested
import org.cafienne.system.CaseSystem

class RootArchiver(caseSystem: CaseSystem, metadata: ActorMetadata) extends RootStorageActor[ArchiveNode](caseSystem, metadata) with LazyLogging {
  override def createInitialEvent: ArchivalRequested = ArchivalRequested(metadata)

  override def storageActorType: Class[_ <: Actor] = classOf[ActorDataArchiver]

  override def createOffspringNode(nodeMetadata: ActorMetadata): ArchiveNode = {
    if (nodeMetadata == metadata) {
      new RootArchiveNode(caseSystem, nodeMetadata, this)
    } else {
      new ArchiveNode(nodeMetadata, this)
    }
  }
}
