package slick
package migration.api
package org.cafienne.service.db.sqlserver

import slick.ast.FieldSymbol
import slick.jdbc.SQLServerProfile
import slick.migration.api.AstHelpers._
import slick.model.ForeignKeyAction

/**
  * Initial implementation of a Dialect for MS SQL Server.
  *
  * Currently overrides all methods, simply invoking super. Changes VARCHAR* into NVARCHAR*.
  */
class SQLServerDialect extends Dialect[SQLServerProfile]{
  override def autoInc(ci: ColumnInfo) = {
    if(ci.autoInc) " AUTO_INCREMENT" else ""
  }


  override def addColumn(table: TableInfo, column: ColumnInfo): String = {
    val statement = super.addColumn(table, column)
    log("addColumn: " + statement)
    statement
  }

  override def addColumnWithInitialValue(table: TableInfo, column: ColumnInfo, rawSqlExpr: String): List[String] = {
    log("addColumnWithInitialValue")
    val statement = super.addColumnWithInitialValue(table, column, rawSqlExpr)
    statement
  }

  override def alterColumnDefault(table: TableInfo, column: ColumnInfo): String = {
    log("alterColumnDefault")
    val statement = super.alterColumnDefault(table, column)
    statement
  }

//  override protected def colInfo[T <: JdbcProfile#Table[_]](table: T)(f: T => Rep[_]): ColumnInfo = {
//    log("colInfo")
//    super.colInfo(table)(f)
//  }

//  override protected def columnInfo(driver: JdbcProfile, column: FieldSymbol): ColumnInfo = {
//    log("columnInfo")
//    super.columnInfo(driver, column)
//  }

//  override def columnList(columns: Seq[FieldSymbol]): String = {
//    log("columnList")
//    super.columnList(columns)
//  }

  override def columnSql(ci: ColumnInfo, newTable: Boolean): String = {
    val statement = super.columnSql(ci, newTable)
    if (ci.isPk) {
//      System.err.println("columnSql(newTable="+newTable+"): " + statement)
    }
    statement
  }
  
  private def log(msg: String): Unit = {
//    System.out.println(msg)
  }

  override def columnType(ci: ColumnInfo) = {
    val statement = super.columnType(ci)
    if (statement.startsWith("VARCHAR")) {
      // ALL VARCHAR becomes NVARCHAR
//      System.err.println("CHANGING THE STATEMNT for " + ci.name+" from " + statement +" into N"+statement)
      "N"+statement
    } else {
      statement
    }
  }

  override def createForeignKey(sourceTable: TableInfo, name: String, sourceColumns: Seq[FieldSymbol], targetTable: TableInfo, targetColumns: Seq[FieldSymbol], onUpdate: ForeignKeyAction, onDelete: ForeignKeyAction): String = {
    val statement = super.createForeignKey(sourceTable, name, sourceColumns, targetTable, targetColumns, onUpdate, onDelete)
    log("createForeignKey: " + statement)
    statement
  }

  override def createIndex(index: IndexInfo) = {
    val statement = super.createIndex(index)
    log("createIndex: "+statement)
    statement
  }

  override def createTable(table: TableInfo, columns: Seq[ColumnInfo]): List[String] = {
    val statement = super.createTable(table, columns)
//    println("\n\ncreateTable["+table.tableName+"]: "+statement+"\n\n")
    statement
  }

  override def dropColumn(table: TableInfo, column: String): List[String] = {

    val statement = super.dropColumn(table, column)
    log("dropColumn: "+statement)
    statement
  }

  override def dropConstraint(table: TableInfo, name: String): String = {

    val statement = super.dropConstraint(table, name)
    log("dropConstraint: "+statement)
    statement
  }

  override def dropTable(table: TableInfo) = {

    val statement = super.dropTable(table)
    log("dropTable: "+statement)
    statement
  }

//  override protected def fieldSym(node: Node) = {
//    log("fieldSym")
//    super.fieldSym(node)
//  }

//  override protected def indexInfo(index: Index) = {
//    log("indexInfo")
//    super.indexInfo(index)
//  }

//  override def migrateTable(table: TableInfo, actions: List[TableMigration.Action]): List[String] = {
//    log("migrateTable")
//    super.migrateTable(table, actions)
//  }

  override def notNull(ci: ColumnInfo) = {
    val statement = super.notNull(ci)
    log("notNull for " + ci.name+": " + statement)
    statement
  }

  override def primaryKey(ci: ColumnInfo, newTable: Boolean): String = {
    val statement = super.primaryKey(ci, newTable)
    log("primaryKey: "+ statement)
    statement
  }

  override def renameIndex(old: IndexInfo, newName: String): List[String] = {
    log("renameIndex")
    val statement = super.renameIndex(old, newName)
    statement
  }

  override def renameTable(table: TableInfo, to: String): String = {
    s""""sp_rename "${quoteTableName(table)}", @newname ${quoteIdentifier(to)}"""
  }

//  override protected def tableInfo(table: TableNode) = {
//    log("tableInfo")
//    super.tableInfo(table)
//  }

  override def dropIndex(index: IndexInfo) = {
    s"drop index ${quoteIdentifier(index.name)} on ${quoteTableName(tableInfo(index.table))}"
  }


//  override def renameColumn(table: TableInfo, from: ColumnInfo, to: String): String = super.renameColumn(table, from, to)

  /**
    */
  override def renameColumn(table: TableInfo, from: String, to: String) = {
    s""""sp_rename "${quoteTableName(table)}.${quoteIdentifier(from)}", @newname ${quoteIdentifier(to)}, @objtype="COLUMN" """
  }

//  override protected def quotedColumnNames(ns: Seq[FieldSymbol]): Seq[String] = super.quotedColumnNames(ns)

//  override def quoteIdentifier(id: String): String = super.quoteIdentifier(id)

//  override def quoteTableName(t: TableInfo) = super.quoteTableName(t)


  override def alterColumnNullability(table: TableInfo, column: ColumnInfo) = {
    val statement = super.alterColumnNullability(table, column)
    log("alterColumnType: "+statement)
    statement
  }

  override def alterColumnType(table: TableInfo, column: ColumnInfo) = {
    val statement = super.alterColumnType(table, column)
    log("alterColumnType: "+statement)
    statement
  }

  override def dropForeignKey(table: TableInfo, name: String) = {

    val statement = super.dropForeignKey(table, name)
    log("dropForeignKey: "+statement)
    statement
  }

  override def createPrimaryKey(table: TableInfo, name: String, columns: Seq[FieldSymbol]) = {

    val statement = super.createPrimaryKey(table, name, columns);
//    System.err.println("createPrimaryKey["+table.tableName+"]: "+statement)
    statement
  }

  override def dropPrimaryKey(table: TableInfo, name: String) = {

    val statement = super.dropPrimaryKey(table, name)
    log("dropPrimaryKey: "+statement)
    statement
  }
}
