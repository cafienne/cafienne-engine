package org.cafienne.infrastructure.eventstore

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.persistence.query.{Offset, Sequence}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Proxy towards an offset storage. The client uses a name that indicates a unique (i.e., singleton)
  * offset storage to find a PersistentActor that keeps track of the offset storage offered to the client.
  * This can be used to e.g. read events from an akka read journal starting from the last known and stored
  * offset, instead of starting from the beginning. It helps avoid replay all events from a stream upon
  * restarting the jvm.
  * @param offsetStorageName unique name
  * @param system Akka system that can be used to create PersistentActor
  * @param ec Execution context to be used for interacting with the akka system
  */
class OffsetStorageClient(offsetStorageName : String)(implicit val system: ActorSystem, val ec: ExecutionContext) {
  implicit val timeout: Timeout = Timeout(15.seconds) // This timeout is given for asking OffsetStorage actors for last known offset

  /**
    * Reference to PersistentActor that keeps track of last known event offset
    */
  val offsetStorage: ActorRef = system.actorOf(Props(classOf[OffsetStorage]), offsetStorageName)

  import akka.pattern.ask

  /**
    * Returns a future with the currently stored offset
    * @return
    */
  def getOffset : Future[Offset] = {
    val offsetStorageAnswer = offsetStorage ? "get" // Actually - anything but Long can be sent in order to get a response...
    offsetStorageAnswer.map {
      case l: Long => Sequence(l);
      case offset: Offset => offset
      case other => throw new IllegalArgumentException("Did not receive a valid Offset from the offset storage, but " + other)
    }
  }

  /**
    * Inform the offset storage of a new offset
    * @param newOffset new offset value
    */
  def setOffset(newOffset : Offset): Unit = {
    offsetStorage ! newOffset // Inform OffsetStorage[...] about new offset
  }
}
