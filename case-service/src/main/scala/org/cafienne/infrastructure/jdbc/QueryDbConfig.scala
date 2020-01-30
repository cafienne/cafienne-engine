package org.cafienne.infrastructure.jdbc

import org.cafienne.akka.actor.CaseSystem
import slick.ast.ColumnOption
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.CanBeQueryCondition
import slick.relational.RelationalProfile.ColumnOption.Length
import slick.sql.SqlProfile.ColumnOption.SqlType

trait QueryDbConfig {
  lazy val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("query-db", CaseSystem.config)

  lazy val db = dbConfig.db

  import dbConfig.profile.api._
  lazy val isSQLServer = dbConfig.profile.isInstanceOf[slick.jdbc.SQLServerProfile]

  abstract class CafienneTable[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {

    /**
      * Creates a String column with the specified name and options.
      * Overrides O.Length and SqlType options, and replaces with NVARCHAR(1024).
      * This method can be used in primary key fields and foreign key fields of type String
      * @param columnName
      * @param options
      * @tparam T
      * @return
      */
    def keyColumn[T](columnName: String, options: ColumnOption[String]*) = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        isSQLServer match {
          // If not SQL Server then just return the existing definition of the column
          case false => options
          // But if it is SQL Server, let's check if the Length is defined. If not, we'll set it to 1024
          case true => options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => {
              options ++ Seq(SqlType("NVARCHAR(1024)"))
            }
          }
        }
      }
      super.column[String](columnName, newOptions:_*)
    }
  }

  implicit class QueryHelper[T, E](query: Query[T, E, Seq]) {
    def optionFilter[X, R: CanBeQueryCondition](name: Option[X])(f: (T, X) => R) =
      name.map(v => query.withFilter(f(_, v))).getOrElse(query)
  }
}
