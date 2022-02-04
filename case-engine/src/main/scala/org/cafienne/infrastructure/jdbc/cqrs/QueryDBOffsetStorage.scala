package org.cafienne.infrastructure.jdbc.cqrs

import org.cafienne.querydb.schema.QueryDBSchema

class QueryDBOffsetStorage(val storageName: String) extends JDBCOffsetStorage with QueryDBSchema {
}
