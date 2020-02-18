package org.cafienne.akka.actor

import org.cafienne.cmmn.instance.casefile.{Value, ValueMap}
import scala.collection.JavaConverters._

class CafienneVersion(val json: ValueMap = Value.convert(org.cafienne.cmmn.akka.BuildInfo.toMap.asJava).asInstanceOf[ValueMap]) {
  /**
    * Returns true if the two versions differ, false if they are the same.
    * @param otherVersionInstance
    * @return
    */
  def differs(otherVersionInstance: CafienneVersion): Boolean = {
    !json.equals(otherVersionInstance.json)
  }

}
