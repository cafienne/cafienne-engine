package org.cafienne.service.api.debug

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.query.EventEnvelope
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.cmmn.akka.event.plan.PlanItemCreated
import org.cafienne.cmmn.instance.casefile.{ValueList, ValueMap}
import org.cafienne.infrastructure.cqrs.ReadJournalProvider

import scala.concurrent.Future

class ModelEventsReader()(implicit override val system: ActorSystem) extends LazyLogging with ReadJournalProvider {

  implicit val executionContext = system.dispatcher

  def getEvents(user: PlatformUser, actorId: String): Future[ValueList] = {
    getEventsRecursively(user, actorId)
  }

  private def getEventsRecursively(user: PlatformUser, actorId: String): Future[ValueList] = {
    val eventList = new ValueList
    val source: Source[EventEnvelope, akka.NotUsed] = journal.currentEventsByPersistenceId(actorId, 0L, Long.MaxValue)
    source.runForeach {
      case EventEnvelope(_, _, sequenceNr: Long, event: ModelEvent[_]) => {
        if (user == null || user.tenants.contains(event.tenant) || user.isPlatformOwner) {
          val eventNr = sequenceNr.asInstanceOf[java.lang.Long]
          val eventType = event.getClass.getSimpleName
          val rawJson = event.rawJson
          eventList.add(new ValueMap("nr", eventNr, "type", eventType, "content", rawJson))
          event match {
            case ec: PlanItemCreated => {
              getEventsRecursively(user, ec.getPlanItemId).flatMap(e => {
                e.forEach(e => eventList.add(e))
                Future.successful{Done}
              })
            }
            case _ => {}
          }
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
