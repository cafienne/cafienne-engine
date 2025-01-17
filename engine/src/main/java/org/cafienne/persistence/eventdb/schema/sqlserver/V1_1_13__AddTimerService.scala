package org.cafienne.persistence.eventdb.schema.sqlserver

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_13__AddTimerService extends ClassicEventDBSchemaScript {

  val version = "1.1.13"

  val description = "AddTimerService"

  val scriptName = "V1_1_13__AddTimerService.sql"

  override def getChecksum: Integer = 235057769

  override def sql: String = {
    s"""DROP TABLE IF EXISTS ${Cafienne.config.persistence.tablePrefix}timer;
       |
       |CREATE TABLE ${Cafienne.config.persistence.tablePrefix}timer (
       |	"timer_id" NVARCHAR(255) NOT NULL,
       |	"case_instance_id" NVARCHAR(255) NOT NULL,
       |	"moment" [datetimeoffset](6) NOT NULL,
       |    "tenant" NVARCHAR(255) NOT NULL,
       |	"user" NVARCHAR(255) NOT NULL,
       |	PRIMARY KEY ("timer_id")
       |);
       |
       |DROP TABLE IF EXISTS ${Cafienne.config.persistence.tablePrefix}offset_storage;
       |
       |CREATE TABLE ${Cafienne.config.persistence.tablePrefix}offset_storage (
       |	"name" NVARCHAR(255) NOT NULL,
       |	"offset-type" NVARCHAR(255) NOT NULL,
       |	"offset-value" NVARCHAR(255) NOT NULL,
       |	"timestamp" [datetimeoffset](6) NOT NULL,
       |    PRIMARY KEY ("name")
       |);""".stripMargin
  }
}
