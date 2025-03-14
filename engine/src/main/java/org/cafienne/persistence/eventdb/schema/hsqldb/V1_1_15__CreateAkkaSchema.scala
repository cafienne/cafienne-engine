package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_15__CreateAkkaSchema extends ClassicEventDBSchemaScript {
  val version = "1.1.15"

  val description = "CreateAkkaSchemaEventTags"

  val scriptName = "org.cafienne.persistence.schema.V1_1_15__CreateAkkaSchemaEventTags"

  override def getChecksum: Integer = 1702145589

  override def sql: String = {
    s"""
       |CREATE TABLE IF NOT EXISTS "PUBLIC"."${Cafienne.config.persistence.tablePrefix}event_tag" (
       |      event_id BIGINT UNIQUE,
       |      tag VARCHAR(256),
       |      PRIMARY KEY(event_id, tag),
       |      FOREIGN KEY(event_id)
       |         REFERENCES "PUBLIC"."${Cafienne.config.persistence.tablePrefix}event_journal"(ordering)
       |         ON DELETE CASCADE
       |);
       |""".stripMargin
  }
}
