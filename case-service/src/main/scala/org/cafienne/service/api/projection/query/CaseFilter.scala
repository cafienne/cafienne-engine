package org.cafienne.service.api.projection.query

case class CaseFilter(tenant: Option[String] = None, caseName: Option[String] = None, status: Option[String] = None, identifiers: Option[String] = None)
