package org.cafienne.akka.actor

import org.cafienne.cmmn.instance.casefile.ValueMap

class HealthMonitor {
  private val statusMap = new ValueMap("Status", "Ok", "Description", "We feel very healthy today")

  def status: String = {
    statusMap.toString
  }
}
