/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.infrastructure.jdbc

import org.cafienne.infrastructure.Cafienne
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import slick.ast.ColumnOption
import slick.basic.DatabaseConfig
import slick.jdbc.{JdbcProfile, SQLServerProfile}
import slick.lifted.{ColumnOrdered, Index}
import slick.migration.api.org.cafienne.infrastructure.jdbc.sqlserver.SQLServerDialect
import slick.migration.api.{Dialect, GenericDialect, MigrationSeq, SqlMigration}
import slick.relational.RelationalProfile.ColumnOption.Length
import slick.sql.SqlAction
import slick.sql.SqlProfile.ColumnOption.SqlType

/**
 * Basic JDBC abstraction on Slick that can be used to hook a database connection
 * based on config properties.
 *
 * Includes some helpers for queries and MS SQL Server support
 */
trait CafienneJDBCConfig {
  lazy val dbConfig: DatabaseConfig[JdbcProfile] = ???

  lazy val db = dbConfig.db

  import dbConfig.profile.api._

  lazy val isSQLServer: Boolean = dbConfig.profile.isInstanceOf[slick.jdbc.SQLServerProfile]

  val prefix: String = Cafienne.config.persistence.tablePrefix

  abstract class CafienneTable[T](tag: Tag, name: String) extends Table[T](tag, prefix + name) {

    /**
     * If queries on the table use the Sort case class, then the table must implement this method
     *
     * @param field
     * @return
     */
    def getSortColumn(field: String): ColumnOrdered[_] = ???

    /**
     * Creates a String column with the specified name and options.
     * Overrides O.Length and SqlType options, and replaces with NVARCHAR(255).
     * This method can be used in primary key fields and foreign key fields of type String
     * Typically in fields that hold actor id's, such as Tenant, CaseInstanceId, or TaskId
     *
     * @param columnName
     * @param options
     * @tparam T
     * @return
     */
    def idColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
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
     * Creates an index on the table on the column, with a standard generation of the index name.
     *
     * @param column
     * @return
     */
    def oldStyleIndex(column: Rep[String]): Index = {
      index(oldStyleIxName(column), column)
    }

    /**
     * Generate an index name for the specified column
     *
     */
    def oldStyleIxName(column: Rep[_]): String = {
      if (prefix.isEmpty) {
        // Old style index generation includes table-name twice. When we also add the prefix it may become too long.
        //  When there is a prefix we make the index name shorter.
        s"ix_${tableName}__$column"
      } else {
        // Use the new ixName method that gives shorted index names
        ixName(column)
      }
    }

    def index(column: Rep[String]): Index = {
      index(ixName(column), column)
    }

    /**
     * Generate an index name for the specified column
     */
    def ixName(column: Rep[_]): String = {
      val columnName = s"$column".replace(s"$tableName.", "")
      val name = s"ix_${tableName}__$columnName"
//      println("Creating index name " + name +"\n\tfor column: '" + column.toString() +"'\n\ton table '"+ tableName +s"'\n\tnode type: '${column.toNode.nodeType}' with node string '${column.toNode.toString()}'\n")
      name
    }

    /**
     * Generate a primary key name based on the table name (to make it unique across the database)
     *
     * @return
     */
    def pkName: String = {
      s"pk_$tableName"
    }

    /**
     * Similar to idColumn, but now for storing userId's. Defaults to 255 as length.
     *
     * @param columnName
     * @param options
     * @tparam T
     * @return
     */
    def userColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
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
     *
     * @param columnName
     * @param options
     * @tparam T
     * @return
     */
    def stateColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
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
     *
     * @param columnName
     * @param options
     * @tparam T
     * @return
     */
    def jsonColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
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

  /**
   * Base class for tables that have a 'tenant' column
   *
   * @param tag
   * @param tableName
   * @tparam T
   */
  abstract class CafienneTenantTable[T](tag: Tag, tableName: String) extends CafienneTable[T](tag, tableName) {
    def tenant: Rep[String] = idColumn[String]("tenant")
  }

  implicit class QueryHelper[CT <: CafienneTable[_], E](query: Query[CT, E, Seq]) {
    /**
     * Orders the results as given in the Sort object.
     * Note that if the Sort.on field is empty (None), the query will not be affected.
     * Default sort order is descending
     *
     * @param sort
     * @return
     */
    def order(sort: Sort): Query[CT, E, Seq] = {
      sort.on.fold(query)(fieldName => sort.ascending match {
        case true => query.sortBy(_.getSortColumn(fieldName.toLowerCase).asc)
        case _ => query.sortBy(_.getSortColumn(fieldName.toLowerCase).desc)
      })
    }

    /**
     * Only select records from a certain offset and up to a certain number of results
     *
     * @param area
     * @return
     */
    def only(area: Area): Query[CT, E, Seq] = {
      query.drop(area.offset).take(area.numOfResults)
    }
  }

  implicit class TenantQueryHelper[CTT <: CafienneTenantTable[_], E](query: Query[CTT, E, Seq]) {
    /**
     * Add tenant selector to the query. If no tenants specified, it will not add a filter on tenant
     * and search across tenants
     *
     * @param tenants
     * @return
     */
    def inTenants(tenants: Option[Seq[String]]): Query[CTT, E, Seq] = {
      val set = tenants.getOrElse(Seq())
      set.isEmpty match {
        case true => query
        case false => query.filter(_.tenant.inSet(set))
      }
    }
  }

  implicit lazy val dialect: Dialect[_ <: JdbcProfile] = {
    dbConfig.profile match {
      case _: SQLServerProfile => new SQLServerDialect
      case _ => GenericDialect(dbConfig.profile)
    }
  }

  /**
   * Helper to create SqlMigration object from a 'regular' Slick SqlAction
   *
   * @param action
   * @return
   */
  def asSqlMigration(action: SqlAction[_, _, _]): MigrationSeq = {
    asSqlMigration(action.statements.toSeq: _*)
  }

  /**
   * Helper to create SqlMigration from one or more strings
   *
   * @param action
   * @return
   */
  def asSqlMigration(sql: String*): MigrationSeq = {
    //    val statements = sql.mkString(";\n")
    //    println(s"SQL:\n\t$statements\n")
    MigrationSeq(sql.map(statement => SqlMigration(statement)): _*)
  }
}
