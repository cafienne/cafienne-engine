package org.cafienne.persistence.eventdb.schema.h2

import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_36__ChangeTimerTablesAddRootCaseId(tablePrefix: String) extends ClassicEventDBSchemaScript {
  val version = "1.1.36"

  val description = "ChangeTimerTablesAddRootCaseId"

  val scriptName = "V1_1_36__ChangeTimerTablesAddRootCaseId.sql"

  override def getChecksum: Integer = -2037901020

  override def sql: String = {
    s"""ALTER TABLE ${tablePrefix}timer ADD COLUMN root_case_id varchar(255) NOT null;
       |CREATE INDEX "${tablePrefix}timer_rootcaseid_idx" on "${tablePrefix}timer" ("root_case_id");
       """.stripMargin
  }
}
