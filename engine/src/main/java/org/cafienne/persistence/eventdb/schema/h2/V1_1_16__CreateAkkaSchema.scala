package org.cafienne.persistence.eventdb.schema.h2

import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_16__CreateAkkaSchema(tablePrefix: String) extends ClassicEventDBSchemaScript {
  val version = "1.1.16"

  val description = "CreateAkkaSchema"

  val scriptName = "org.cafienne.persistence.schema.V1_1_16__CreateAkkaSchema"

  val sql: String =
    s"""CREATE TABLE IF NOT EXISTS "${tablePrefix}event_journal" (
       |    "ordering" BIGINT NOT NULL AUTO_INCREMENT,
       |    "deleted" BOOLEAN DEFAULT false NOT NULL,
       |    "persistence_id" VARCHAR(255) NOT NULL,
       |    "sequence_number" BIGINT NOT NULL,
       |    "writer" VARCHAR NOT NULL,
       |    "write_timestamp" BIGINT NOT NULL,
       |    "adapter_manifest" VARCHAR NOT NULL,
       |    "event_payload" BLOB NOT NULL,
       |    "event_ser_id" INTEGER NOT NULL,
       |    "event_ser_manifest" VARCHAR NOT NULL,
       |    "meta_payload" BLOB,
       |    "meta_ser_id" INTEGER,
       |    "meta_ser_manifest" VARCHAR,
       |    PRIMARY KEY("persistence_id","sequence_number")
       |    );
       |
       |CREATE UNIQUE INDEX "${tablePrefix}event_journal_ordering_idx" on "${tablePrefix}event_journal" ("ordering");
       |
       |CREATE TABLE IF NOT EXISTS "${tablePrefix}event_tag" (
       |    "event_id" BIGINT NOT NULL,
       |    "tag" VARCHAR NOT NULL,
       |    PRIMARY KEY("event_id", "tag"),
       |    CONSTRAINT fk_${tablePrefix}event_journal
       |      FOREIGN KEY("event_id")
       |      REFERENCES "event_journal"("ordering")
       |      ON DELETE CASCADE
       |);
       |
       |CREATE TABLE IF NOT EXISTS "${tablePrefix}snapshot" (
       |    "persistence_id" VARCHAR(255) NOT NULL,
       |    "sequence_number" BIGINT NOT NULL,
       |    "created" BIGINT NOT NULL,"snapshot_ser_id" INTEGER NOT NULL,
       |    "snapshot_ser_manifest" VARCHAR NOT NULL,
       |    "snapshot_payload" BLOB NOT NULL,
       |    "meta_ser_id" INTEGER,
       |    "meta_ser_manifest" VARCHAR,
       |    "meta_payload" BLOB,
       |    PRIMARY KEY("persistence_id","sequence_number")
       |    );
       |
       |CREATE TABLE IF NOT EXISTS "${tablePrefix}durable_state" (
       |    "global_offset" BIGINT NOT NULL AUTO_INCREMENT,
       |    "persistence_id" VARCHAR(255) NOT NULL,
       |    "revision" BIGINT NOT NULL,
       |    "state_payload" BLOB NOT NULL,
       |    "state_serial_id" INTEGER NOT NULL,
       |    "state_serial_manifest" VARCHAR,
       |    "tag" VARCHAR,
       |    "state_timestamp" BIGINT NOT NULL,
       |    PRIMARY KEY("persistence_id")
       |    );
       |
       |CREATE INDEX "${tablePrefix}state_tag_idx" on "${tablePrefix}durable_state" ("tag");
       |CREATE INDEX "${tablePrefix}state_global_offset_idx" on "${tablePrefix}durable_state" ("global_offset");
       |""".stripMargin
}
