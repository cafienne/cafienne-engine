package org.cafienne.persistence.eventdb.schema.postgres

import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_16__CreateAkkaSchema(tablePrefix: String) extends ClassicEventDBSchemaScript {
  val version = "1.1.16"

  val description = "CreateAkkaSchema"

  val scriptName = "org.cafienne.persistence.schema.V1_1_16__CreateAkkaSchema"

  override def getChecksum: Integer = 1702145590

  override def sql: String = {
    s"""CREATE TABLE IF NOT EXISTS public.${tablePrefix}event_journal(
       |  ordering BIGSERIAL,
       |  persistence_id VARCHAR(255) NOT NULL,
       |  sequence_number BIGINT NOT NULL,
       |  deleted BOOLEAN DEFAULT FALSE NOT NULL,
       |
       |  writer VARCHAR(255) NOT NULL,
       |  write_timestamp BIGINT,
       |  adapter_manifest VARCHAR(255),
       |
       |  event_ser_id INTEGER NOT NULL,
       |  event_ser_manifest VARCHAR(255) NOT NULL,
       |  event_payload BYTEA NOT NULL,
       |
       |  meta_ser_id INTEGER,
       |  meta_ser_manifest VARCHAR(255),
       |  meta_payload BYTEA,
       |
       |  PRIMARY KEY(persistence_id, sequence_number)
       |);
       |
       |CREATE UNIQUE INDEX ${tablePrefix}event_journal_ordering_idx ON public.${tablePrefix}event_journal(ordering);
       |
       |CREATE TABLE IF NOT EXISTS public.${tablePrefix}event_tag(
       |    event_id BIGINT,
       |    tag VARCHAR(256),
       |    PRIMARY KEY(event_id, tag),
       |    CONSTRAINT fk_${tablePrefix}event_journal
       |      FOREIGN KEY(event_id)
       |      REFERENCES ${tablePrefix}event_journal(ordering)
       |      ON DELETE CASCADE
       |);
       |
       |CREATE TABLE IF NOT EXISTS public.${tablePrefix}snapshot (
       |  persistence_id VARCHAR(255) NOT NULL,
       |  sequence_number BIGINT NOT NULL,
       |  created BIGINT NOT NULL,
       |
       |  snapshot_ser_id INTEGER NOT NULL,
       |  snapshot_ser_manifest VARCHAR(255) NOT NULL,
       |  snapshot_payload BYTEA NOT NULL,
       |
       |  meta_ser_id INTEGER,
       |  meta_ser_manifest VARCHAR(255),
       |  meta_payload BYTEA,
       |
       |  PRIMARY KEY(persistence_id, sequence_number)
       |);
       |
       |CREATE TABLE IF NOT EXISTS public.${tablePrefix}durable_state (
       |    global_offset BIGSERIAL,
       |    persistence_id VARCHAR(255) NOT NULL,
       |    revision BIGINT NOT NULL,
       |    state_payload BYTEA NOT NULL,
       |    state_serial_id INTEGER NOT NULL,
       |    state_serial_manifest VARCHAR(255),
       |    tag VARCHAR,
       |    state_timestamp BIGINT NOT NULL,
       |    PRIMARY KEY(persistence_id)
       |    );
       |CREATE INDEX ${tablePrefix}state_tag_idx on public.${tablePrefix}durable_state (tag);
       |CREATE INDEX ${tablePrefix}state_global_offset_idx on public.${tablePrefix}durable_state (global_offset);
       |""".stripMargin
  }
}
