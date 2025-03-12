package org.cafienne.actormodel.communication.request.state

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.{Cancellable, Scheduler}
import org.cafienne.system.CaseSystem

import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class RequestTracker(val request: Request, val caseSystem: CaseSystem) extends Runnable with LazyLogging {
  private implicit val dispatcher: ExecutionContext = caseSystem.system.dispatcher
  private val scheduler: Scheduler = caseSystem.system.scheduler
  private var retry: Option[Cancellable] = None
  private val maxAttempts = 10
  private var count = 0L

  def run(): Unit = {
    if (count > maxAttempts) {
      logger.warn(s"Maximum number $maxAttempts of attempts to send request is reached. Timer-retry mechanism is canceled.")
      stop()
      request.failed()
    } else {
      logger.warn(s"Retrying (attempt $count) to send request $request to ${request.getCommand.getActorId} for which so far no delivery confirmation was received")
      request.send()
    }
  }

  def start(): Unit = {
    if (count <= maxAttempts) {
      stop() // First stop any current schedules
      count += 1
//      println("Increased count of " + request +"["+request.getCommand.getMessageId+"] to " + count)
      // Slowly start delaying
      retry = Some(scheduler.scheduleOnce(Duration.create(1 * count, TimeUnit.SECONDS), this))
    }
  }

  def stop(): Unit = {
//    println("Stopping tracker for " + request +"["+request.getCommand.getMessageId+"] on count " + count)
    retry.foreach(_.cancel());
  }
}
