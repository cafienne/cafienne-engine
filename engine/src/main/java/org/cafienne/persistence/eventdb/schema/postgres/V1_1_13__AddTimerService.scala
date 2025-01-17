package org.cafienne.persistence.eventdb.schema.postgres

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_13__AddTimerService extends ClassicEventDBSchemaScript {

  val version = "1.1.13"

  val scriptName = "org.cafienne.persistence.schema.V1_1_13__AddTimerService"

  val description = "AddTimerService"

  override def getChecksum: Integer = 1905604043

  override def sql: String = {
    s"""DROP TABLE IF EXISTS ${Cafienne.config.persistence.tablePrefix}timer;
       |
       |CREATE TABLE ${Cafienne.config.persistence.tablePrefix}timer (
       |	"timer_id" character varying COLLATE pg_catalog."default" NOT NULL,
       |	"case_instance_id" character varying COLLATE pg_catalog."default" NOT NULL,
       |	"moment" timestamp without time zone NOT NULL,
       |    "tenant" character varying COLLATE pg_catalog."default" NOT NULL,
       |	"user" character varying COLLATE pg_catalog."default" NOT NULL,
       |
       |	CONSTRAINT ${Cafienne.config.persistence.tablePrefix}timer_pkey PRIMARY KEY (timer_id)
       |);
       |
       |DROP TABLE IF EXISTS ${Cafienne.config.persistence.tablePrefix}offset_storage;
       |
       |CREATE TABLE ${Cafienne.config.persistence.tablePrefix}offset_storage (
       |	"name" character varying COLLATE pg_catalog."default" NOT NULL,
       |	"offset-type" character varying COLLATE pg_catalog."default" NOT NULL,
       |	"offset-value" character varying COLLATE pg_catalog."default" NOT NULL,
       |	"timestamp" timestamp without time zone NOT NULL,
       |
       |	CONSTRAINT ${Cafienne.config.persistence.tablePrefix}offset_pkey PRIMARY KEY (name)
       |);""".stripMargin
  }
}
