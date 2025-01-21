package org.cafienne.persistence.infrastructure.jdbc.schema

import org.cafienne.persistence.infrastructure.jdbc.SlickTableExtensions
import slick.jdbc.{JdbcProfile, SQLServerProfile}
import slick.migration.api.org.cafienne.persistence.infrastructure.jdbc.sqlserver.SQLServerDialect
import slick.migration.api.{Dialect, GenericDialect, MigrationSeq, SqlMigration}
import slick.sql.SqlAction

trait SlickMigrationExtensions extends SlickTableExtensions {

  implicit val dialect: Dialect[_ <: JdbcProfile] = {
    dbConfig.profile match {
      case _: SQLServerProfile => new SQLServerDialect
      case _ => GenericDialect(dbConfig.profile)
    }
  }

  /**
   * Helper to create SqlMigration object from a 'regular' Slick SqlAction
   */
  def asSqlMigration(action: SqlAction[_, _, _]): MigrationSeq = {
    asSqlMigration(action.statements.toSeq: _*)
  }

  /**
   * Helper to create SqlMigration from one or more strings
   */
  def asSqlMigration(sql: String*): MigrationSeq = {
    //    val statements = sql.mkString(";\n")
    //    println(s"SQL:\n\t$statements\n")
    MigrationSeq(sql.map(statement => SqlMigration(statement)): _*)
  }
}
