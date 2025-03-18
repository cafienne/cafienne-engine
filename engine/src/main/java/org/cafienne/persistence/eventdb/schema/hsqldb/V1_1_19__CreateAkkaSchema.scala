package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.eventdb.schema.ClassicEventDBSchemaScript

class V1_1_19__CreateAkkaSchema extends ClassicEventDBSchemaScript {
  val version = "1.1.19"

  val description = "CreateAkkaSchemaOffsetIndex"

  val scriptName = "org.cafienne.persistence.schema.V1_1_19__CreateAkkaSchemaOffsetIndex"

  override def getChecksum: Integer = 1702145593

  override def sql: String = {
    s"""CREATE INDEX "PUBLIC"."${Cafienne.config.persistence.tablePrefix}state_global_offset_idx" on "PUBLIC"."${Cafienne.config.persistence.tablePrefix}durable_state" (global_offset);""".stripMargin
  }
}
