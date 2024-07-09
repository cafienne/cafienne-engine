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

import org.apache.pekko.persistence.query.{NoOffset, Offset, Sequence, TimeBasedUUID}

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
  * Simple and serializable wrapper to keep track of an journal offset by name.
  * Each projection that reads journal events can keep track of "latest handled event"
  * using the projection name to uniquely identify the projection.
  * Underneath the NamedOffset persists itself inside a JDBC table (through Slick)
  */
case class OffsetRecord(name: String, offsetType: String, offsetValue: String, timestamp: Timestamp = Timestamp.from(Instant.now)) {
  def asOffset(): Offset = {
    offsetType match {
      case "TimeBasedUUID" => try {
        TimeBasedUUID(UUID.fromString(offsetValue))
      } catch {
        case iae: IllegalArgumentException => {
          // Do some serious logging here with LazyLogging
          //          System.err.println("Invalid offset value "+offsetValue+", returning no offset")
          iae.printStackTrace()
          Offset.noOffset
        }
        case t: Throwable => {
          //          System.err.println("Cannot convert the value of the offset "+this+" due to some weird error ", t)
          throw t
        }
      }
      case "Sequence" =>
        offsetValue.isBlank match {
          case false => Sequence(offsetValue.toLong)
          case true => Offset.noOffset
        }
      case "None" => Offset.noOffset
      case other => {
        //        System.err.println("Offset of type "+offsetType+" is not recognized; returning no offset")
        Offset.noOffset
      }
    }
  }
}

object OffsetRecord {
  /**
    * Creates a new record for storage of offset
    */
  def apply(offsetName: String, offset: Offset): OffsetRecord = {
    offset match {
      case uuid: TimeBasedUUID => OffsetRecord(offsetName, "TimeBasedUUID", uuid.value.toString)
      case seq: Sequence => OffsetRecord(offsetName, "Sequence", seq.value.toString)
      case NoOffset => OffsetRecord(offsetName, "None", "0")
      case other => {
        throw new RuntimeException("Cannot handle offsets of type " + other.getClass.getName)
      }
    }
  }
}
