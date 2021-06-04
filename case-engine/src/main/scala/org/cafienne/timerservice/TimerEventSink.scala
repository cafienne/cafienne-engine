package org.cafienne.timerservice

import akka.Done
import akka.actor.ActorSystem
import akka.persistence.query.Offset
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.akka.actor.command.response.{CommandFailure, ModelResponse}
import org.cafienne.akka.actor.event.ModelEvent
import org.cafienne.cmmn.akka.event.plan.eventlistener._
import org.cafienne.infrastructure.cqrs.{OffsetStorage, TaggedEventConsumer}
import org.cafienne.timerservice.persistence.{TimerStore, TimerStoreProvider}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TimerEventSink(val timerService: TimerService)(implicit val caseSystem: CaseSystem, implicit val system: ActorSystem) extends TaggedEventConsumer {

  val storage: TimerStore = new TimerStoreProvider()(system).store
  override val offsetStorage: OffsetStorage = storage
  override val tag: String = TimerBaseEvent.TAG
  private val schedule: mutable.Map[String, Scheduled] = mutable.Map()

  def open() = {
    storage.getTimers().map(timers => {
      logger.info(s"Scheduling batch with ${timers.length} timers upon startup.")
      loadTimerBatch(timers)
      start()
    })
  }

  private def loadTimerBatch(timers: Seq[Timer]) = {
    timers.foreach(scheduleTimer)
  }

  private def scheduleTimer(job: Timer) = {
    schedule.put(job.timerId, new Scheduled(timerService, job, this))
  }

  def setTimer(event: TimerSet, offset: Offset): Future[Done] = {
    logger.debug(s"${event.getClass.getSimpleName} on timer ${event.getTimerId} in case ${event.getActorId} (triggering at ${event.getTargetMoment})")
    val job: Timer = Timer(event.getCaseInstanceId, event.getTimerId, event.getTargetMoment, event.getUser)
    scheduleTimer(job)
    storage.storeTimer(job, Some(offset))
  }

  def removeTimer(event: TimerCleared, offset: Offset): Future[Done] = {
    logger.debug(s"${event.getClass.getSimpleName} on timer ${event.getTimerId} in case ${event.getActorId}")
    removeTimer(event.getTimerId, Some(offset))
  }

  def removeTimer(timerId: String): Future[Done] = {
    removeTimer(timerId, None)
  }

  def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    schedule.remove(timerId).map(schedule => schedule.cancel)
    storage.removeTimer(timerId, offset)
  }

  def handleFailingCaseInvocation(job: Scheduled, failure: CommandFailure) = {
    // TODO: we can also update the timer state in the storage???
    logger.warn(s"Could not trigger timer ${job.timer.timerId} in case ${job.timer.caseInstanceId}:" + failure.toJson)
  }

  def handleCaseInvocation(job: Scheduled, response: ModelResponse) = {
    // TODO: we can also delete the timer here, or update a state for that timer in the store
    logger.whenDebugEnabled(logger.debug(s"Successfully invoked timer ${job.timer.timerId} in case ${job.timer.caseInstanceId}"))
  }

  def migrateTimers(timers: java.util.List[Timer]): Unit = {
    import scala.jdk.CollectionConverters._
    // Note: Migration MUST be done synchronously, otherwise the flow of starting and opening up to listen to the stream
    //  may fail if timers are added async and already read when opening the stream.
    storage.importTimers(timers.asScala)
  }

  /**
    * This method must be implemented by the consumer to handle the tagged ModelEvent
    *
    * @param offset
    * @param persistenceId
    * @param sequenceNr
    * @param modelEvent
    * @return
    */
  override def consumeModelEvent(offset: Offset, persistenceId: String, sequenceNr: Long, modelEvent: ModelEvent[_]): Future[Done] = {
    modelEvent match {
      case event: TimerSet => setTimer(event, offset)
      case event: TimerCleared => removeTimer(event, offset)
      case other => {
        logger.warn(s"Timer Service received an unexpected event of type ${other.getClass.getName}")
        Future.successful(Done)
      }
    }
  }
}
