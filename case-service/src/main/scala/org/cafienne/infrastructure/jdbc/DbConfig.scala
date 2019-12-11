package org.cafienne.infrastructure.jdbc

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.CanBeQueryCondition

trait DbConfig {
  lazy val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("projectionsDB")

  lazy val db = dbConfig.db

  import dbConfig.profile.api._

  implicit class QueryHelper[T, E](query: Query[T, E, Seq]) {
    def optionFilter[X, R: CanBeQueryCondition](name: Option[X])(f: (T, X) => R) =
      name.map(v => query.withFilter(f(_, v))).getOrElse(query)
  }
}
