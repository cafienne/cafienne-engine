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

package org.cafienne.system.health

import org.cafienne.json.ValueMap

import java.util
import scala.collection.mutable.Set
import scala.jdk.CollectionConverters._

/**
  * Health monitor has latest status information on health of the Case System
  */
object HealthMonitor {

  // Make it an ordered set, so that the json structure is stable.
  private val measures: Set[HealthMeasurePoint] = new util.LinkedHashSet[HealthMeasurePoint]().asScala

  val queryDB = addMeasure("query-db")
  val idp = addMeasure("idp")
  val writeJournal = addMeasure("write-journal", false)
  val readJournal = addMeasure("read-journal")
  val timerService = addMeasure("timer-service", false)

  private def description = "Health indication of the Case Engine is currently " + health

  private def health: String = if (ok()) "OK" else "NOK"

  def ok(): Boolean = {
    measures.find(p => p.isCritical && p.unhealthy()).forall(_ => false)
  }

  def report: ValueMap = {
    val json = new ValueMap("Status", health, "Description", description)
    val points = json.withArray("measure-points")
    measures.foreach(measure => points.add(new ValueMap(measure.key, measure.asJSON())))
    json
  }

  def addMeasure(key: String, isCritical: Boolean = true): HealthMeasurePoint = {
    val measure = new HealthMeasurePoint(key, isCritical)
    measures += measure
    measure
  }
}
