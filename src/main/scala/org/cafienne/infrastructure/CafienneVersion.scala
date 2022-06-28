package org.cafienne.infrastructure

import org.cafienne.BuildInfo
import org.cafienne.json.{JSONReader, Value, ValueMap}

// Note: json creation is somewhat cumbersome, but it is required in order to have the comparison mechanism work properly.
class CafienneVersion(val json: ValueMap = JSONReader.parse(Value.convert(BuildInfo.toMap).asMap.toString).asInstanceOf[ValueMap]) {
  /**
    * Returns true if the two versions differ, false if they are the same.
    *
    * @param otherVersionInstance
    * @return
    */
  def differs(otherVersionInstance: CafienneVersion): Boolean = {
    !json.equals(otherVersionInstance.json)
  }

  override def toString: String = json.toString
}
