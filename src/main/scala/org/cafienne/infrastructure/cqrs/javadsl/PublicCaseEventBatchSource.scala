package org.cafienne.infrastructure.cqrs.javadsl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.Offset
import akka.stream.javadsl
import org.cafienne.infrastructure.cqrs.batch.public_events.PublicCaseEventBatch

import scala.concurrent.Future

class PublicCaseEventBatchSource(system: ActorSystem, offsetStorage: OffsetStorage) {

  private class Wrapper(override val system: ActorSystem, offsetStorage: OffsetStorage) extends org.cafienne.infrastructure.cqrs.batch.PublicCaseEventBatchSource {
    override def getOffset: Future[Offset] = offsetStorage.getOffset
  }

  private val wrapper = new Wrapper(system, offsetStorage)

  /**
    * Get a stream (Source) of public Events for java
    *
    * @return Source of PublicCaseEventBatches
    */
  def publicEvents(): javadsl.Source[PublicCaseEventBatch, NotUsed] = wrapper.publicEvents.asJava

}
