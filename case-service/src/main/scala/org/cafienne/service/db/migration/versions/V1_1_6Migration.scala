package org.cafienne.service.db.migration.versions

import org.cafienne.service.api.cases.table.CaseTables
import org.cafienne.service.db.migration.SlickQueryDbMigrationConfig
import slick.migration.api.flyway.{MigrationInfo, VersionedMigration}
import slick.migration.api.{Migration, TableMigration}

object V1_1_6Migration extends SlickQueryDbMigrationConfig
  with CaseTables
  with CaseTablesV1 {

  //Seq[VersionedMigration]
  override def getMigrations(implicit infoProvider: MigrationInfo.Provider[Migration]): Seq[VersionedMigration[String]] = {
    import dbConfig.profile.api._



    val dropPK = TableMigration(TableQuery[CaseInstanceTeamMemberTableV1]).dropPrimaryKeys(_.pk)

    // Add 2 new columns for memberType ("user" or "role") and case ownership
    //  Existing members all get memberType "user" and also all of them get ownership.
    //  Ownership is needed, because otherwise no one can change the case team anymore...
    // Also we rename columns role and user_id to caseRole and memberId (since member is not just user but can also hold a tenant role)
    val enhanceCaseTeamTable = TableMigration(TableQuery[CaseInstanceTeamMemberTable])
      .renameColumnFrom("user_id", _.memberId)
      .renameColumnFrom("role", _.caseRole)
      .addColumnAndSet(_.isTenantUser,  true)
      .addColumnAndSet(_.isOwner,  true)

    val addPK = TableMigration(TableQuery[CaseInstanceTeamMemberTable]).addPrimaryKeys(_.pk)

    val version = "1.1.6" // Should this not simply take BuildInfo.version???
    Seq(VersionedMigration(version, dropPK & enhanceCaseTeamTable & addPK))
  }
}
