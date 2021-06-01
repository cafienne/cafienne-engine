package org.cafienne.timerservice.persistence.inmemory

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.timerservice.{Timer, TimerStorage}
import org.cafienne.timerservice.persistence.TimerStore

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class InMemoryStore extends TimerStore with LazyLogging {
  val timers = new mutable.HashMap[String, Timer]()

  override def getOffset(): Future[Offset] = Future.successful(Offset.noOffset)

  override def getTimers(): Future[Seq[Timer]] = {
    Future.successful(timers.values.toSeq)
  }

  override def storeTimer(job: Timer, offset: Option[Offset]): Future[Done] = {
    timers.put(job.timerId, job)
    logger.debug(s"Stored timer $job")
    Future.successful(Done)
  }

  override def removeTimer(timerId: String, offset: Option[Offset]): Future[Done] = {
    timers.remove(timerId)
    logger.debug(s"Removed timer $timerId")
    Future.successful(Done)
  }

  override def importTimers(list: Seq[Timer]): Unit = {
    list.foreach(timer => timers.put(timer.timerId, timer))
  }
}
