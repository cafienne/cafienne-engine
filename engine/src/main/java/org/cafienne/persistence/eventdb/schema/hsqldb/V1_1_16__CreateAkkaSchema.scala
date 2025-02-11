package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_16__CreateAkkaSchema extends ClassicEventDBSchemaScript {
  val version = "1.1.16"

  val description = "CreateAkkaSchemaSnapshot"

  val scriptName = "org.cafienne.persistence.schema.V1_1_16__CreateAkkaSchemaSnapshot"

  override def getChecksum: Integer = 1702145590

  override def sql: String = {
    s"""CREATE TABLE IF NOT EXISTS "PUBLIC"."${Cafienne.config.persistence.tablePrefix}snapshot" (
       |  persistence_id VARCHAR(255) NOT NULL,
       |  sequence_number BIGINT NOT NULL,
       |  created BIGINT NOT NULL,
       |
       |  snapshot_ser_id INTEGER NOT NULL,
       |  snapshot_ser_manifest VARCHAR(255) NOT NULL,
       |  snapshot_payload BLOB NOT NULL,
       |
       |  meta_ser_id INTEGER,
       |  meta_ser_manifest VARCHAR(255),
       |  meta_payload BLOB,
       |
       |  PRIMARY KEY(persistence_id, sequence_number)
       |);
       |""".stripMargin
  }
  //CREATE INDEX "PUBLIC"."${Cafienne.config.persistence.tablePrefix}state_tag_idx" on "PUBLIC"."${Cafienne.config.persistence.tablePrefix}durable_state" (tag);
  //CREATE INDEX "PUBLIC"."${Cafienne.config.persistence.tablePrefix}state_global_offset_idx" on "PUBLIC"."${Cafienne.config.persistence.tablePrefix}durable_state" (global_offset);
}
