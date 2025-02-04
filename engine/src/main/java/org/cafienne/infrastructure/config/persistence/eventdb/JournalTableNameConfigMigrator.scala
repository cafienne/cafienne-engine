package org.cafienne.infrastructure.config.persistence.eventdb

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.util.ConfigMigrator

class JournalTableNameConfigMigrator extends ConfigMigrator {
  override def run(config: Config): Config = {
    val prefixKey = "cafienne.persistence.table-prefix"

    if (!config.hasPath(prefixKey)) {
      return config
    }

    val prefix = config.getString(prefixKey).trim()
    if (prefix.isEmpty) {
      return config
    }

    logger.warn(s"Running migration on pekko table names to include prefix $prefix")

    // Tagging is configured in the akka persistence journal.
    //  This journal has different configuration keys per type of persistence.
    //  Find the right path based on the config of the journal plugin.
    def migrateTableName(config: Config, key: String, tableName: String): Config = {
      migrateConfigurationValue(config, s"$key.tables.$tableName", "tableName", tableName, prefix + tableName, showWarnings = false)
    }

    var newConfig = config
    newConfig = migrateTableName(newConfig, "jdbc-journal", tableName = "event_journal")
    newConfig = migrateTableName(newConfig, "jdbc-journal", tableName = "event_tag")
    newConfig = migrateTableName(newConfig, "jdbc-read-journal", tableName = "event_journal")
    newConfig = migrateTableName(newConfig, "jdbc-read-journal", tableName = "event_tag")
    newConfig = migrateTableName(newConfig, "jdbc-snapshot-store", tableName = "snapshot")
    newConfig = migrateTableName(newConfig, "jdbc-durable-state-store", tableName = "durable_state")
    newConfig
  }
}
