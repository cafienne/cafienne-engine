package org.cafienne.persistence.eventdb.schema.h2

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_13__AddTimerService extends ClassicEventDBSchemaScript {

  val version = "1.1.13"

  val scriptName = "org.cafienne.persistence.schema.V1_1_13__AddTimerService"

  val description = "AddTimerService"

  override def sql: String = {
    s"""CREATE TABLE IF NOT EXISTS PUBLIC."${Cafienne.config.persistence.tablePrefix}timer" (
       |  "timer_id" VARCHAR NOT NULL,
       |  "case_instance_id" VARCHAR NOT NULL,
       |  "moment" TIMESTAMP NOT NULL,
       |  "tenant" VARCHAR NOT NULL,
       |  "user" VARCHAR NOT NULL,
       |  PRIMARY KEY("timer_id")
       |);
       |
       |DROP TABLE IF EXISTS PUBLIC."${Cafienne.config.persistence.tablePrefix}offset_storage";
       |
       |CREATE TABLE PUBLIC."offset_storage" (
       |	"name" VARCHAR NOT NULL,
       |	"offset-type" VARCHAR NOT NULL,
       |	"offset-value" VARCHAR NOT NULL,
       |	"timestamp" TIMESTAMP NOT NULL,
       |    PRIMARY KEY ("name")
       |);
       |""".stripMargin
  }
}
