package org.cafienne.service.db.migration

import org.cafienne.infrastructure.jdbc.QueryDbConfig
import slick.jdbc.{JdbcProfile, SQLServerProfile}
import slick.migration.api.org.cafienne.service.db.sqlserver.SQLServerDialect
import slick.migration.api.{Dialect, GenericDialect}

/**
  * Dialect provider that also supports Microsoft SQL Server
  */
trait QueryDbMigrationConfig extends QueryDbConfig {
  implicit val dialect = getDialect(dbConfig.profile)

  private def getDialect(profile: JdbcProfile) = {
    ExtendedGenericDialect(profile)
  }
}

object ExtendedGenericDialect {
  def apply(driver: JdbcProfile): Dialect[_ <: JdbcProfile] = driver match {
    case _: SQLServerProfile    => new SQLServerDialect
    case _ => GenericDialect(driver)
  }
}
