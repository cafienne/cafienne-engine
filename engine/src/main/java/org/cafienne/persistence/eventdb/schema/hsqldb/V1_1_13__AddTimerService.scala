package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_13__AddTimerService extends ClassicEventDBSchemaScript {

  val version = "1.1.13"

  val scriptName = "org.cafienne.persistence.schema.V1_1_13__AddTimerService"

  val description = "AddTimerService"

  override def getChecksum: Integer = 1905604043

  override def sql: String = {
    s"""CREATE TABLE IF NOT EXISTS "PUBLIC"."${Cafienne.config.persistence.tablePrefix}timer" (
       |  "timer_id" VARCHAR(50) NOT NULL PRIMARY KEY,
       |  "case_instance_id" VARCHAR(50) NOT NULL,
       |  "moment" TIMESTAMP NOT NULL,
       |  "tenant" VARCHAR(50) NOT NULL,
       |  "user" VARCHAR(100) NOT NULL,
       |);
       |
       |CREATE TABLE IF NOT EXISTS "PUBLIC"."${Cafienne.config.persistence.tablePrefix}offset_storage" (
       |	"name" VARCHAR(255) NOT NULL PRIMARY KEY,
       |	"offset-type" VARCHAR(255) NOT NULL,
       |	"offset-value" VARCHAR(255) NOT NULL,
       |	"timestamp" timestamp without time zone NOT NULL,
       |);""".stripMargin
  }
}
