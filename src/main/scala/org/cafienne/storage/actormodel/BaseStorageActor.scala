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

package org.cafienne.storage.actormodel

import akka.persistence.PersistentActor
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.system.CaseSystem

trait BaseStorageActor
  extends PersistentActor
    with StorageActorSupervisor
    with LazyLogging {
  val caseSystem: CaseSystem
  val metadata: ActorMetadata

  def printLogMessage(msg: String): Unit = {
    Printer.print(this.metadata, msg)
  }

  def reportUnknownMessage(msg: Any): Unit = {
    logger.warn(s"$metadata: Received message with unknown type. Ignoring it. Message is of type ${msg.getClass.getName}")
  }
}

/**
  * Simplistic console printer. To be replaced with proper log infrastructure (whenDebugEnabled and such)
  */
object Printer extends LazyLogging {
  var previousActor: ActorMetadata = _

  def out(msg: String): Unit = {
    logger.whenDebugEnabled(logger.debug(msg))
  }

  def print(metadata: ActorMetadata, msg: String): Unit = {
    val path = if (metadata == previousActor) "- " else {
      val root = if (metadata.isRoot) "ROOT " else ""
      "\n" + root + metadata.path + ":\n- "
    }

    if (msg.startsWith("\n====") || msg.startsWith("====")) {
      out(msg)
    } else if (msg.startsWith("\n")) {
      out(s"\n$path${msg.substring(1)}")
    } else {
      out(s"$path$msg")
    }
    previousActor = metadata
  }
}
