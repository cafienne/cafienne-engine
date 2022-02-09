package org.cafienne.infrastructure.jdbc.query

case class Sort(on: Option[String], direction: Option[String] = Some("desc")) {
  lazy val ascending = direction.fold(false)(d => if (d matches "(?i)asc")  true else false)
}

object Sort {
  def NoSort: Sort = Sort(None, None)

  def asc(field: String) = Sort(Some(field), Some("asc"))

  def on(field: String) = Sort(Some(field))

  def withDefault(on: Option[String], direction: Option[String], defaultOnField: String) = Sort(Some(on.getOrElse(defaultOnField)), direction)
}
