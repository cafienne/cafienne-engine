package org.cafienne.infrastructure.eventstore

import java.util.UUID

import akka.persistence.query.{Offset, Sequence, TimeBasedUUID}
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import akka.serialization.SerializerWithStringManifest
import com.typesafe.scalalogging.LazyLogging
import enumeratum._
import OffsetType.{NoOffsetType, SequenceType, TimeBasedUUIDType}

sealed trait OffsetType extends EnumEntry

object OffsetType extends Enum[OffsetType] {

  val values = findValues

  case object TimeBasedUUIDType extends OffsetType
  case object SequenceType extends OffsetType
  case object NoOffsetType extends OffsetType
}


case class WrappedOffset(offsetType: OffsetType, offsetValue: String = "")

class OffsetSerializer extends SerializerWithStringManifest with LazyLogging {
  import OffsetType._

  override def identifier: Int = 6768

  private final val delimiter = ":"

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    val bytesAsString = new String(bytes)
    try {
      val delPos = bytesAsString.indexOf(delimiter)
      OffsetType.withName(bytesAsString.substring(0,delPos)) match {
        case TimeBasedUUIDType => WrappedOffset(TimeBasedUUIDType, bytesAsString.substring(delPos + 1))
        case SequenceType => WrappedOffset(SequenceType, bytesAsString.substring(delPos + 1))
        case NoOffsetType => WrappedOffset(NoOffsetType)
      }
    } catch {
      case err: Throwable =>
        logger.warn("Failed to convert the bytes into a Long, therefore returning 0. Perhaps the bytes have been tampered externally? Bytes are: " + bytesAsString, err)
        Offset.noOffset
    }
  }

  override def manifest(o: AnyRef): String = {
    "Offset"
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    o match {
      case o: WrappedOffset =>
        (o.offsetType.toString + delimiter + o.offsetValue.toString).getBytes
      case _ =>
        logger.error("Cannot serialize objects of type " + o.getClass.getName)
        throw new RuntimeException("WrappedOffsetSerializer can only serialize WrappedOffset(s)")
    }
  }
}


/**
  * Persistent actor that keeps track of an event sourcing offset.
  * Actor can receive 2 commands: either a Long (indicating a new offset) or anything else.
  * When the Long offset is received, it will be updated and stored in the Actor. For any other
  * message received, the actor will return the current offset.
  */
class OffsetStorage extends PersistentActor with LazyLogging {

  private var currentOffset = Offset.noOffset

  override def receiveRecover: Receive = {
    case _: RecoveryCompleted => logger.debug("Recovered " + this) // initialize further processing when required
    case SnapshotOffer(_, newOffset: WrappedOffset) => updateState(unwrap(newOffset))
    case other => logger.error("received unknown event to recover:" + other)
  }

  override def receiveCommand: Receive = {
    case newNumber: Offset =>
      logger.debug("Updating " + this + " -> " + newNumber)

      // TODO: This will make the snapshot storage grow forever (snapshots aren't deleted)
      saveSnapshot(wrap(newNumber)) // Note: we do not persist events, only store the snapshot
      updateState(newNumber)
    case _ => sender ! currentOffset
  }

  private def wrap(offset: Offset): WrappedOffset = {
    offset match  {
      case uuid: TimeBasedUUID => WrappedOffset(TimeBasedUUIDType, uuid.value.toString)
      case seq: Sequence => WrappedOffset(SequenceType, seq.value.toString)
      case _ => WrappedOffset(NoOffsetType)
    }
  }

  private def unwrap(wrappedOffset: WrappedOffset): Offset = {
    wrappedOffset.offsetType match {
      case TimeBasedUUIDType => TimeBasedUUID(UUID.fromString(wrappedOffset.offsetValue))
      case SequenceType => Sequence(wrappedOffset.offsetValue.toLong)
      case NoOffsetType => Offset.noOffset
    }
  }

  private def updateState(newHighNumber: Offset): Unit = {
    currentOffset = newHighNumber
  }

  override def toString: String = {
    "OffsetStorage[" + persistenceId + "] = " + currentOffset
  }

  override def persistenceId: String = {
    self.path.name
  }
}
