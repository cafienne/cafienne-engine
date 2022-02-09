package org.cafienne.service.akkahttp.cases

import org.cafienne.json.{CafienneJson, Value, ValueMap}

final case class CaseList(caseName: String = "",
                          totalInstances: Long = 0,
                          numActive:Long = 0,
                          numCompleted:Long = 0,
                          numTerminated:Long = 0,
                          numSuspended:Long = 0,
                          numFailed:Long = 0,
                          numClosed:Long = 0,
                          numWithFailures: Long = 0) extends CafienneJson {

  override def toValue: Value[_] = {
    val v = new ValueMap
    v.plus("caseName", caseName)
    v.plus("totalInstances", totalInstances)

    v.plus("numActive", numActive)
    v.plus("numCompleted", numCompleted)
    v.plus("numTerminated", numTerminated)

    v.plus("numSuspended", numSuspended)
    v.plus("numFailed", numFailed)
    v.plus("numClosed", numClosed)
    v.plus("numWithFailures", numWithFailures)
    v
  }
}
