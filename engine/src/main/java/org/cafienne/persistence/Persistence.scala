package org.cafienne.persistence

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.EventDB
import org.cafienne.infrastructure.config.persistence.eventdb.JournalTableNameConfigMigrator
import org.cafienne.infrastructure.config.util.SystemConfig
import org.cafienne.persistence.querydb.schema.QueryDB

object Persistence {
  def initializeDatabaseSchemas(): Unit = {
    SystemConfig.addMigrators(new JournalTableNameConfigMigrator)
    if (Cafienne.config.persistence.initializeDatabaseSchemas) {
      QueryDB.initializeDatabaseSchema()
      EventDB.initializeDatabaseSchema()
    }
  }
}
