/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
