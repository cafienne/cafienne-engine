package org.cafienne.persistence.eventdb.schema.sqlserver

import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_28__ChangeTimerTablesToVarChar(tablePrefix: String) extends ClassicEventDBSchemaScript {
  val version = "1.1.28"

  val description = "ChangeTimerTablesToVarChar"

  val scriptName = "V1_1_28__ChangeTimerTablesToVarChar.sql"

  override def getChecksum: Integer = -2037901020

  override def sql: String = {
    s"""DECLARE @${tablePrefix}timer_pk_name VARCHAR(255);
       |(SELECT @${tablePrefix}timer_pk_name = CONSTRAINT_NAME
       |    FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
       |    WHERE TABLE_NAME = '${tablePrefix}timer'
       |    AND CONSTRAINT_TYPE = 'PRIMARY KEY');
       |
       |EXEC('ALTER TABLE [${tablePrefix}timer] DROP CONSTRAINT ' + @${tablePrefix}timer_pk_name);
       |
       |ALTER TABLE [${tablePrefix}timer] ALTER COLUMN [timer_id] varchar(255) NOT null;
       |ALTER TABLE [${tablePrefix}timer] ALTER COLUMN [case_instance_id] varchar(255) NOT null;
       |ALTER TABLE [${tablePrefix}timer] ALTER COLUMN [tenant] varchar(255) NOT null;
       |ALTER TABLE [${tablePrefix}timer] ALTER COLUMN [user] varchar(255) NOT null;
       |
       |ALTER TABLE ${tablePrefix}timer ADD PRIMARY KEY (timer_id);
       |
       |
       |DECLARE @offset_storage_pk_name NVARCHAR(255);
       |(SELECT @offset_storage_pk_name = CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME = '${tablePrefix}offset_storage' AND CONSTRAINT_TYPE = 'PRIMARY KEY');
       |EXEC('ALTER TABLE [${tablePrefix}offset_storage] DROP CONSTRAINT ' + @offset_storage_pk_name);
       |
       |ALTER TABLE [${tablePrefix}offset_storage] ALTER COLUMN [name] varchar(255) NOT null;
       |ALTER TABLE [${tablePrefix}offset_storage] ALTER COLUMN [offset-type] varchar(255) NOT null;
       |ALTER TABLE [${tablePrefix}offset_storage] ALTER COLUMN [offset-value] varchar(255) NOT null;
       |
       |ALTER TABLE ${tablePrefix}offset_storage ADD PRIMARY KEY (name);""".stripMargin
  }
}
