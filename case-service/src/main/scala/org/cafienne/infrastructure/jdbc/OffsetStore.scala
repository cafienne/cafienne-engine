package org.cafienne.infrastructure.jdbc

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import akka.persistence.query.{NoOffset, Offset, Sequence, TimeBasedUUID}

/**
  * Simple object to keep track of an Akka Persistence Offset by name.
  * Each Projection reading Akka Persistence events can keep track of "latest handled event"
  * using the name to uniquely identify the Projection.
  * Underneath the OffsetStore persists itself inside a JDBC table (through Slick)
  * @param name
  * @param offsetType
  * @param offsetValue
  * @param timestamp
  */
case class OffsetStore(name: String, offsetType: String, offsetValue: String, timestamp: Timestamp =  Timestamp.from(Instant.now)) {
  def asOffset() : Offset = {
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
      case "Sequence" => Sequence(offsetValue.toLong)
      case "None" => Offset.noOffset
      case other => {
//        System.err.println("Offset of type "+offsetType+" is not recognized; returning no offset")
        Offset.noOffset
      }
    }
  }
}

object OffsetStore {
  /**
    * Creates a new record for storage of offset
    * @param offsetName
    * @param offset
    * @return
    */
  def fromOffset(offsetName: String, offset: Offset) : OffsetStore = {
    offset match {
      case uuid: TimeBasedUUID => OffsetStore(offsetName, "TimeBasedUUID", uuid.value.toString)
      case seq: Sequence => OffsetStore(offsetName, "Sequence", seq.value.toString)
      case NoOffset => OffsetStore(offsetName, "None", "0")
      case other =>  {
        throw new RuntimeException("Cannot handle offsets of type "+other.getClass.getName)
      }
    }
  }

}
