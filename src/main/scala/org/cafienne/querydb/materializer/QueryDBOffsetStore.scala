package org.cafienne.querydb.materializer

import org.cafienne.infrastructure.jdbc.cqrs.JDBCOffsetStorage
import org.cafienne.querydb.schema.QueryDBSchema

case class QueryDBOffsetStore(override val storageName: String) extends QueryDBSchema with JDBCOffsetStorage
