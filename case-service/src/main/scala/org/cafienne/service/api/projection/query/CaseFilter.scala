package org.cafienne.service.api.projection.query

case class CaseFilter(tenant: Option[String] = None, definition: Option[String] = None, status: Option[String] = None, identifiers: Option[String] = None)
