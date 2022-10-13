package org.cafienne.system.health

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.json.ValueMap

import java.time.Instant

class HealthMeasurePoint(val key: String, val isCritical: Boolean) extends LazyLogging {
  private var healthy = true
  private var description = key + " is healthy "
  private var changed: Instant = _

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
    logger.error(s"$key reported bad health", throwable)

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
      json.plus("changed-at", changed)
    }
    json
  }
}
