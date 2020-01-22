package org.cafienne.infrastructure.jdbc

import org.cafienne.akka.actor.CaseSystem
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.CanBeQueryCondition

trait QueryDbConfig {
  lazy val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("query-db", CaseSystem.config)

  lazy val db = dbConfig.db

  import dbConfig.profile.api._

  implicit class QueryHelper[T, E](query: Query[T, E, Seq]) {
    def optionFilter[X, R: CanBeQueryCondition](name: Option[X])(f: (T, X) => R) =
      name.map(v => query.withFilter(f(_, v))).getOrElse(query)
  }
}
