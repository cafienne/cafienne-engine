package org.cafienne.persistence

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.EventDB
import org.cafienne.querydb.schema.QueryDB

object Persistence {
  def initializeDatabaseSchemas(): Unit = {
    if (Cafienne.config.persistence.initializeDatabaseSchemas) {
      EventDB.initializeDatabaseSchema()
      QueryDB.initializeDatabaseSchema()
    }
  }
}
