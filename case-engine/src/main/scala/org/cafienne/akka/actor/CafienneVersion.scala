package org.cafienne.akka.actor

import org.cafienne.akka.actor.serialization.json.{JSONReader, ValueMap}
import org.cafienne.cmmn.akka.BuildInfo

class CafienneVersion(val json: ValueMap = JSONReader.parse(BuildInfo.toJson).asInstanceOf[ValueMap]) {
  /**
    * Returns true if the two versions differ, false if they are the same.
    * @param otherVersionInstance
    * @return
    */
  def differs(otherVersionInstance: CafienneVersion): Boolean = {
    !json.equals(otherVersionInstance.json)
  }

  override def toString: String = json.toString
}
