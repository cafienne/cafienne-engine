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

package org.cafienne.infrastructure.cqrs.offset

import org.apache.pekko.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future

/**
  * Simple storage for event offset of a certain projection.
  */
trait OffsetStorage extends LazyLogging {
  /**
    * Unique name of the storage.
    */
  val storageName: String

  /**
    * Gets the latest known offset from the storage
    *
    * @return
    */
  def getOffset: Future[Offset]

  /**
    * Creates a record for the given offset with the storage name. Does not store the record.
    *
    * @param offset
    * @return
    */
  def createOffsetRecord(offset: Offset): OffsetRecord = OffsetRecord(storageName, offset)
}
