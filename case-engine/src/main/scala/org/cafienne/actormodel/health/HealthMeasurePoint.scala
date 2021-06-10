package org.cafienne.actormodel.health

import java.time.Instant
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.json.ValueMap

class HealthMeasurePoint(val key: String) extends LazyLogging {
  private var healthy = true
  private var description = key + " is healthy "
  private var changed: Instant = null

  def unhealthy(): Boolean = !healthy

  /**
    * Report that the measure point is healthy.
    */
  def isOK(): Unit = {
    if (! healthy) {
      logger.error("{} is healthy health again", key)
      healthy = true
      description = key + " is healthy "
      changed = Instant.now
    }
  }

  /**
    * Report that the measure point has failed
    * @param throwable
    */
  def hasFailed(throwable: Throwable): Unit = {
    logger.error("{} reported bad health", key, throwable)

    healthy = false
    description = "Failure: " + throwable.getLocalizedMessage
    changed = Instant.now
  }

  def status: String = {
    if (healthy) "OK"
    else "NOK"
  }

  def asJSON(): ValueMap = {
    val json = new ValueMap("Status", status, "Description", description)
    if (changed != null) {
      json.putRaw("changed-at", changed)
    }
    json
  }
}
