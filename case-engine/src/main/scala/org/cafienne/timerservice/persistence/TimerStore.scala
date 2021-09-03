package org.cafienne.timerservice.persistence

import akka.Done
import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.timerservice.Timer

import scala.concurrent.{ExecutionContext, Future}

/**
  * Storage for timer jobs
  */
trait TimerStore extends LazyLogging {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  def getOffset(): Future[Offset]

  val storageName: String = "Timer Service Offset"

  def importTimers(list: Seq[Timer]): Unit

  def getTimers(): Future[Seq[Timer]]

  def storeTimer(job: Timer, offset: Option[Offset]): Future[Done]

  def removeTimer(timerId: String, offset: Option[Offset]): Future[Done]

  /**
    * Return a description for this type of timer store, defaults to simple class name
    */
  lazy val description: String = getClass.getSimpleName
}
