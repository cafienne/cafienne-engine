package org.cafienne.timerservice

import akka.Done
import akka.persistence.query.Offset
import org.cafienne.cmmn.actorapi.event.plan.eventlistener._
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, TaggedEventConsumer}
import org.cafienne.system.CaseSystem

import scala.concurrent.Future

class TimerEventSink(val timerService: TimerService) extends TaggedEventConsumer {
  override val caseSystem: CaseSystem = timerService.caseSystem

  override def getOffset(): Future[Offset] = timerService.storage.getOffset()
  override val tag: String = TimerBaseEvent.TAG

  override def consumeModelEvent(envelope: ModelEventEnvelope): Future[Done] = {
    envelope.event match {
      case event: TimerSet =>
        logger.debug(s"${event.getClass.getSimpleName} on timer ${event.getTimerId} in case ${event.getActorId} (triggering at ${event.getTargetMoment})")
        timerService.monitor.setTimer(event, envelope.offset)
      case event: TimerCleared =>
        logger.debug(s"${event.getClass.getSimpleName} on timer ${event.getTimerId} in case ${event.getActorId}")
        timerService.monitor.removeTimer(event.getTimerId, Some(envelope.offset))
      case other =>
        logger.warn(s"Timer Service received an unexpected event of type ${other.getClass.getName}")
        Future.successful(Done)
    }
  }
}
