package org.cafienne.service.api.debug

import akka.actor.ActorSystem
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.json.{ValueList, ValueMap}
import org.cafienne.json.ValueList
import org.cafienne.infrastructure.cqrs.{OffsetRecord, ReadJournalProvider}

import scala.concurrent.Future

class ModelEventsReader()(implicit override val system: ActorSystem) extends LazyLogging with ReadJournalProvider {

  implicit val executionContext = system.dispatcher

  def getEvents(user: PlatformUser, actorId: String, from: Long, to: Long): Future[ValueList] = {
    val eventList = new ValueList
    val source: Source[EventEnvelope, akka.NotUsed] = journal.currentEventsByPersistenceId(actorId, from, to)
    source.runForeach {
      case EventEnvelope(offset, _, sequenceNr: Long, event: ModelEvent[_]) => {
        if (user == null || user.tenants.contains(event.tenant) || user.isPlatformOwner) {
          val eventNr = sequenceNr.asInstanceOf[java.lang.Long]
          val eventType = event.getClass.getSimpleName
          eventList.add(new ValueMap("nr", eventNr, "offset", OffsetRecord("", offset).offsetValue, "type", eventType, "content", event.rawJson))
        }
      }
      case _ => {
        // ignoring other events
      }
    }.flatMap{ _ =>
      Future{eventList}
    }
  }
}
