package org.cafienne.querydb.query.filter

case class IdentifierFilter(tenant: Option[String] = None, name: Option[String] = None)
