package org.cafienne.infrastructure.jdbc

import org.cafienne.akka.actor.CaseSystem
import slick.ast.ColumnOption
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.{AbstractTable, CanBeQueryCondition, Index}
import slick.relational.RelationalProfile.ColumnOption.Length
import slick.sql.SqlProfile.ColumnOption.SqlType

trait QueryDbConfig {
  lazy val dbConfig: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig("", CaseSystem.config.queryDB.config)

  lazy val db = dbConfig.db

  import dbConfig.profile.api._
  lazy val isSQLServer = dbConfig.profile.isInstanceOf[slick.jdbc.SQLServerProfile]

  abstract class CafienneTable[T](tag: Tag, tableName: String) extends Table[T](tag, tableName) {

    /**
      * Creates a String column with the specified name and options.
      * Overrides O.Length and SqlType options, and replaces with NVARCHAR(255).
      * This method can be used in primary key fields and foreign key fields of type String
      * Typically in fields that hold actor id's, such as Tenant, CaseInstanceId, or TaskId
      * @param columnName
      * @param options
      * @tparam T
      * @return
      */
    def idColumn[T](columnName: String, options: ColumnOption[String]*) = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        isSQLServer match {
          // If not SQL Server then just return the existing definition of the column
          case false => options
          // But if it is SQL Server, let's check if the Length is defined. If not, we'll set it to 1024
          case true => options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => {
              options ++ Seq(SqlType("NVARCHAR(255)"))
            }
          }
        }
      }
      super.column[String](columnName, newOptions:_*)
    }

    /**
      * Creates an index on the table on the column, with a standard generation of the index name.
      * @param column
      * @return
      */
    def index(column: Rep[String]): Index = {
      index(generateIndexName(column), column)
    }

    def generateIndexName(column: Rep[_]): String = {
      s"ix_${tableName}__$column"
    }

    /**
      * Similar to idColumn, but now for storing userId's. Defaults to 255 as length.
      * @param columnName
      * @param options
      * @tparam T
      * @return
      */
    def userColumn[T](columnName: String, options: ColumnOption[String]*) = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        isSQLServer match {
          // If not SQL Server then just return the existing definition of the column
          case false => options
          // But if it is SQL Server, let's check if the Length is defined. If not, we'll set it to 1024
          case true => options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => {
              options ++ Seq(SqlType("NVARCHAR(255)"))
            }
          }
        }
      }
      super.column[String](columnName, newOptions: _*)
    }

    /**
      * Similar to idColumn, but now for storing states and transitions (technical data). Defaults to 16 as length.
      * (ParentTerminate is longest value as of now)
      * @param columnName
      * @param options
      * @tparam T
      * @return
      */
    def stateColumn[T](columnName: String, options: ColumnOption[String]*) = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        isSQLServer match {
          // If not SQL Server then just return the existing definition of the column
          case false => options
          // But if it is SQL Server, let's check if the Length is defined. If not, we'll set it to 1024
          case true => options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => {
              options ++ Seq(SqlType("VARCHAR(16)"))
            }
          }
        }
      }
      super.column[String](columnName, newOptions: _*)
    }


    /**
      * Column that can hold json information; NVARCHAR(MAX) by default
      * @param columnName
      * @param options
      * @tparam T
      * @return
      */
    def jsonColumn[T](columnName: String, options: ColumnOption[String]*) = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        isSQLServer match {
          // If not SQL Server then just return the existing definition of the column
          case false => options
          // But if it is SQL Server, let's check if the Length is defined. If not, we'll set it to 1024
          case true => options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => {
              options ++ Seq(SqlType("NVARCHAR(MAX)"))
            }
          }
        }
      }
      super.column[String](columnName, newOptions: _*)
    }
  }

  implicit class QueryHelper[T, E](query: Query[T, E, Seq]) {
    def optionFilter[X, R: CanBeQueryCondition](name: Option[X])(f: (T, X) => R) =
      name.map(v => query.withFilter(f(_, v))).getOrElse(query)
  }
}
