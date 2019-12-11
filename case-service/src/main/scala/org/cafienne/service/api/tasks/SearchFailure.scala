package org.cafienne.service.api.tasks

case class SearchFailure(msg: String) extends RuntimeException(msg)
