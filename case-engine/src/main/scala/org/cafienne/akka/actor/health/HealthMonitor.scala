package org.cafienne.akka.actor.health

import java.util

import org.cafienne.akka.actor.serialization.json.ValueMap

import scala.collection.mutable.Set
import scala.collection.JavaConverters._

/**
  * Health monitor has latest status information on health of the Case System
  */
object HealthMonitor {

  // Make it an ordered set, so that the json structure is stable.
  private val measures: Set[HealthMeasurePoint] = new util.LinkedHashSet[HealthMeasurePoint]().asScala

  val queryDB = addMeasure("query-db")
  val idp = addMeasure("idp")
  val writeJournal = addMeasure("write-journal")
  val readJournal = addMeasure("read-journal")

  private def description = "Health indication of the Case Engine is currently " + health

  private def health = if (ok) "OK" else "NOK"

  def ok(): Boolean = {
    measures.find(p => p.unhealthy).map(_ => false).getOrElse(true)
  }

  def report: ValueMap = {
    val json = new ValueMap("Status", health, "Description", description)
    val points = json.withArray("measure-points")
    measures.foreach(measure => points.add(new ValueMap(measure.key, measure.asJSON)))
    json
  }

  def addMeasure(key: String): HealthMeasurePoint = {
    val measure = new HealthMeasurePoint(key)
    measures += measure
    measure
  }
}
