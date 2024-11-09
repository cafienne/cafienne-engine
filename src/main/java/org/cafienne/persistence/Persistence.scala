package org.cafienne.persistence

import org.cafienne.querydb.schema.QueryDB

object Persistence {
  def initializeDatabaseSchemas(): Unit = {
    EventDB.initializeDatabaseSchema()
    QueryDB.initializeDatabaseSchema()
  }
}
