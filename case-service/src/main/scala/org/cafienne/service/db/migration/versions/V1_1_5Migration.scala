package org.cafienne.service.db.migration.versions

import org.cafienne.infrastructure.jdbc.OffsetStoreTables
import org.cafienne.service.api.cases.table.CaseTables
import org.cafienne.service.api.tasks.TaskTables
import org.cafienne.service.api.tenant.TenantTables
import org.cafienne.service.db.migration.SlickQueryDbMigrationConfig
import slick.lifted.TableQuery
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}
import slick.migration.api.{Migration, SqlMigration, TableMigration}

object V1_1_5Migration extends SlickQueryDbMigrationConfig
  with OffsetStoreTables {

  // This "migration" removes the CaseProjectionWriter's offset, in order for that projection to be rebuilt.
  //  This enables proper creation of records case_instance_team_member table
  override def getMigrations(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]] = {
    val offsetStoreTable = TableMigration(TableQuery[OffsetStoreTable])
    val removeOffset = SqlMigration(s"""DELETE FROM "${offsetStoreTable.tableInfo.schemaName.fold("")(s => s + ".") + offsetStoreTable.tableInfo.tableName}" where "name" = 'CaseProjectionsWriter' """)
    val mig = VersionedMigration("1.1.5",
      removeOffset
    )
    Seq(mig)
  }
}

