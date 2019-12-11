package org.cafienne.service

package object api {

  final val CASE_LAST_MODIFIED = "Case-Last-Modified"

  case class Sort(sortBy: String, sortOrder: Option[String])
}
