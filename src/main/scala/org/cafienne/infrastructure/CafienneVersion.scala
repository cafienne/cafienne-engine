package org.cafienne.infrastructure

import org.cafienne.BuildInfo
import org.cafienne.json.{Value, ValueMap}

class CafienneVersion(val json: ValueMap = Value.convert(BuildInfo.toMap).asMap) {
  val description: String = {
    var headCommit = json.get("gitHeadCommit").toString
    if (headCommit.startsWith("Some(")) {
      headCommit = headCommit.substring(5, headCommit.length - 1)
    }
    if (json.get("gitCurrentBranch").equals(headCommit)) {
      json.get("version").getValue.toString
    } else {
      "branch " + json.get("gitCurrentBranch") + " (" + json.get("version") + ")"
    }
  }

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
