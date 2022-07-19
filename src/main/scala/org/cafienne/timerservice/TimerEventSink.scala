package org.cafienne.timerservice

import akka.Done
import akka.persistence.query.Offset
import akka.stream.scaladsl.Sink
import org.cafienne.cmmn.actorapi.event.plan.eventlistener._
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventSource}
import org.cafienne.system.CaseSystem
import org.cafienne.system.health.HealthMonitor

import scala.concurrent.Future
import scala.util.{Failure, Success}

class TimerEventSink(val timerService: TimerService) extends TaggedEventSource {
  val caseSystem: CaseSystem = timerService.caseSystem
  override val system = caseSystem.system

  override def getOffset: Future[Offset] = timerService.storage.getOffset
  override val tag: String = TimerBaseEvent.TAG

  def consumeModelEvent(envelope: ModelEventEnvelope): Future[Done] = {
    envelope.event match {
      case event: TimerSet =>
        logger.debug(s"${event.getClass.getSimpleName} on timer ${event.getTimerId} in case ${event.getActorId} (triggering at ${event.getTargetMoment})")
        timerService.monitor.addTimer(event, envelope.offset)
      case event: TimerCleared =>
        logger.debug(s"${event.getClass.getSimpleName} on timer ${event.getTimerId} in case ${event.getActorId}")
        timerService.monitor.removeTimer(event.getTimerId, Some(envelope.offset))
      case other =>
        logger.warn(s"Timer Service received an unexpected event of type ${other.getClass.getName}")
        Future.successful(Done)
    }
  }

  /**
    * Start reading and processing events
    */
  def start(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    taggedEvents
      .mapAsync(1)(consumeModelEvent)
      .runWith(Sink.ignore)
      .onComplete {
        case Success(_) => //
        case Failure(ex) =>
          // No need to print the stack trace itself here, as that is done in HealthMonitor as well.
          logger.error(s"${getClass.getSimpleName} bumped into an issue that it cannot recover from: ${ex.getMessage}")
          HealthMonitor.readJournal.hasFailed(ex)
      }
  }
}
