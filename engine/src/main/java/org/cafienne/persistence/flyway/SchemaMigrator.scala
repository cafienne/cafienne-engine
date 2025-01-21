package org.cafienne.persistence.flyway

import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.executor.MigrationExecutor
import org.flywaydb.core.api.resolver.ResolvedMigration


trait SchemaMigrator extends ResolvedMigration {
  def version: String

  override def getVersion: MigrationVersion = MigrationVersion.fromVersion(version)

  def description: String

  override def getDescription: String = description

  /**
   * @return The name of the script, this will end up in the "script" column of the flyway history table
   */
  def scriptName: String

  override def getScript: String = scriptName

  override def getPhysicalLocation: String = getClass.getName

  override def getExecutor: MigrationExecutor = new MigrationRunner(this)

  override def getChecksum: Integer = null

  override def checksumMatches(checksum: Integer): Boolean = checksum == getChecksum

  override def checksumMatchesWithoutBeingIdentical(checksum: Integer): Boolean = checksumMatches(checksum)

  /**
   * @return The actual SQL script that will be executed
   */
  def sql: String
}
