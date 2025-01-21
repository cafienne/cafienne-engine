package org.cafienne.persistence.flyway

import com.typesafe.scalalogging.LazyLogging
import org.flywaydb.core.api.executor.{Context, MigrationExecutor}

class MigrationRunner(migration: SchemaMigrator) extends MigrationExecutor with LazyLogging {
  override def execute(context: Context): Unit = {
    val sql = migration.sql
    val select = context.getConnection.createStatement
    logger.info(s"======================= Script for '${migration.getClass.getSimpleName}'\n$sql\n=======================")
    select.execute(sql)
  }

  override def canExecuteInTransaction: Boolean = true

  override def shouldExecute(): Boolean = true
}
