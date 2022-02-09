package org.cafienne.infrastructure.jdbc.query

case class Area(offset: Int, numOfResults: Int)

object Area {
  val Default = Area(0, 100)
}
