package com.casefabric.persistence

import com.casefabric.querydb.schema.QueryDB

object Persistence {
  def initializeDatabaseSchemas(): Unit = {
    EventDB.initializeDatabaseSchema()
    QueryDB.initializeDatabaseSchema()
  }
}
