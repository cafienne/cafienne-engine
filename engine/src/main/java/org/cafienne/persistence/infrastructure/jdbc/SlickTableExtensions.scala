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

package org.cafienne.persistence.infrastructure.jdbc

import org.cafienne.infrastructure.Cafienne
import slick.ast.ColumnOption
import slick.lifted.{ColumnOrdered, Index}
import slick.relational.RelationalProfile.ColumnOption.Length
import slick.sql.SqlProfile.ColumnOption.SqlType

/**
 * Basic JDBC abstraction on Slick that can be used to hook a database connection
 * based on config properties.
 *
 */
trait SlickTableExtensions extends SlickDatabaseProfile {
  import dbConfig.profile.api._

  val prefix: String = Cafienne.config.persistence.tablePrefix

  abstract class CafienneTable[T](tag: Tag, name: String) extends Table[T](tag, prefix + name) {

    /**
     * If queries on the table use the Sort case class, then the table must implement this method
     */
    def getSortColumn(field: String): ColumnOrdered[_] = ???

    /**
     * Creates a String column with the specified name and options.
     * Overrides O.Length and SqlType options, and replaces with NVARCHAR(255).
     * This method can be used in primary key fields and foreign key fields of type String
     * Typically in fields that hold actor id's, such as Tenant, CaseInstanceId, or TaskId
     */
    def idColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        if (isSQLServer) {
          options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => options ++ Seq(SqlType("NVARCHAR(255)"))
          }
        } else {
          options
        }
      }
      super.column[String](columnName, newOptions: _*)
    }

    /**
     * Creates an index on the table on the column, with a standard generation of the index name.
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
     */
    def userColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        if (isSQLServer) {
          options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => options ++ Seq(SqlType("NVARCHAR(255)"))
          }
        } else {
          options
        }
      }
      super.column[String](columnName, newOptions: _*)
    }

    /**
     * Similar to idColumn, but now for storing states and transitions (technical data). Defaults to 16 as length.
     * (ParentTerminate is longest value as of now)
     */
    def stateColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        if (isSQLServer) {
          options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => options ++ Seq(SqlType("VARCHAR(16)"))
          }
        } else {
          options
        }
      }
      super.column[String](columnName, newOptions: _*)
    }


    /**
     * Column that can hold json information; NVARCHAR(MAX) by default
     */
    def jsonColumn[C](columnName: String, options: ColumnOption[String]*): Rep[String] = {
      // NOTE: This code is invoked many times, also continuously during runtime

      val newOptions = {
        if (isSQLServer) {
          options.find(o => o.isInstanceOf[Length]) match {
            case Some(_) => options
            case None => options ++ Seq(SqlType("NVARCHAR(MAX)"))
          }
        } else {
          options
        }
      }
      super.column[String](columnName, newOptions: _*)
    }
  }

  /**
   * Base class for tables that have a 'tenant' column
   */
  abstract class CafienneTenantTable[T](tag: Tag, tableName: String) extends CafienneTable[T](tag, tableName) {
    def tenant: Rep[String] = idColumn[String]("tenant")
  }
}
