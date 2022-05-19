package org.cafienne.service.akkahttp.debug

import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.ModelEvent
import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.infrastructure.cqrs.ReadJournalProvider
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.json.{ValueList, ValueMap}
import org.cafienne.system.CaseSystem

import scala.concurrent.{ExecutionContextExecutor, Future}

class ModelEventsReader(val caseSystem: CaseSystem) extends LazyLogging with ReadJournalProvider {
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  def getEvents(user: PlatformUser, actorId: String, from: Long, to: Long): Future[ValueList] = {
    val eventList = new ValueList
    val source: Source[EventEnvelope, akka.NotUsed] = journal().currentEventsByPersistenceId(actorId, from, to)
    source.runForeach {
      case EventEnvelope(offset, _, sequenceNr: Long, event: ModelEvent) =>
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
