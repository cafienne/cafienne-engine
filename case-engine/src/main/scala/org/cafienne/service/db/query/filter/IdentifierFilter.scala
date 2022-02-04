package org.cafienne.service.db.query.filter

case class IdentifierFilter(tenant: Option[String] = None, name: Option[String] = None)
