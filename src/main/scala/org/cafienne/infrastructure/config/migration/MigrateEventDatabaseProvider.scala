package org.cafienne.infrastructure.config.migration

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.util.ConfigMigrator

object MigrateEventDatabaseProvider extends ConfigMigrator {
  override def run(config: Config): Config = {
    // Tagging is configured in the akka persistence journal.
    //  This journal has different configuration keys per type of persistence.
    //  Find the right path based on the config of the journal plugin.
    val deprecatedValue = "org.cafienne.service.db.events.EventDatabaseProvider"
    val newValue = "org.cafienne.journal.jdbc.EventDatabaseProvider"
    migrateConfigurationValue(config, "akka-persistence-jdbc", "database-provider-fqcn", deprecatedValue, newValue, showWarningOnDifferentValue = false)
  }
}
