package org.cafienne.akka.actor

import org.cafienne.akka.actor.serialization.json.{JSONReader, ValueMap}
import org.cafienne.cmmn.akka.BuildInfo

class CafienneVersion(val json: ValueMap = JSONReader.parse(BuildInfo.toJson).asInstanceOf[ValueMap]) {
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
