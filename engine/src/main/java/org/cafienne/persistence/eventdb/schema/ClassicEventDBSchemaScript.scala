package org.cafienne.persistence.eventdb.schema

import org.cafienne.persistence.flyway.SchemaMigrator
import org.flywaydb.core.api.CoreMigrationType

trait ClassicEventDBSchemaScript extends SchemaMigrator {
  override def getType = CoreMigrationType.SQL
}
