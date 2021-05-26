package org.cafienne.service.api.debug

import akka.actor.ActorSystem
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.akka.actor.serialization.json.{ValueList, ValueMap}
import org.cafienne.infrastructure.cqrs.{OffsetRecord, ReadJournalProvider}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ModelEventsReader()(implicit override val system: ActorSystem) extends LazyLogging with ReadJournalProvider {

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def getEvents(user: PlatformUser, actorId: String, from: Long, to: Long): Future[ValueList] = {
    val eventList = new ValueList
    val source: Source[EventEnvelope, akka.NotUsed] = journal().currentEventsByPersistenceId(actorId, from, to)
    source.runForeach {
      case EventEnvelope(offset, _, sequenceNr: Long, event: ModelEvent[_]) =>
        if (user == null || user.tenants.contains(event.tenant) || user.isPlatformOwner) {
          val eventNr = sequenceNr.asInstanceOf[java.lang.Long]
          val eventType = event.getClass.getSimpleName
          eventList.add(new ValueMap("nr", eventNr, "offset", OffsetRecord("", offset).offsetValue, "type", eventType, "content", event.rawJson))
        }
      case _ => {
        // ignoring other events
      }
    }.flatMap{ _ =>
      Future{eventList}
    }
  }
}
