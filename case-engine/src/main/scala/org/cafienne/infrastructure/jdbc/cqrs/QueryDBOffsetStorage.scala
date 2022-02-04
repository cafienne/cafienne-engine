package org.cafienne.infrastructure.jdbc.cqrs

import org.cafienne.service.db.schema.QueryDBSchema

class QueryDBOffsetStorage(val storageName: String) extends JDBCOffsetStorage with QueryDBSchema {
}
