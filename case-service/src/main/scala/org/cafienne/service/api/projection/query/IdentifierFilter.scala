package org.cafienne.service.api.projection.query

case class IdentifierFilter(tenant: Option[String] = None, name: Option[String] = None)
