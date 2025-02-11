package org.cafienne.persistence.eventdb.schema.sqlserver

import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.flyway.SchemaMigrator

object SQLServerEventDBSchema extends EventDBSchema  {
  override def scripts(tablePrefix: String): Seq[SchemaMigrator] = Seq(new V1_1_13__AddTimerService(tablePrefix: String), new V1_1_16__CreateAkkaSchema(tablePrefix: String), new V1_1_28__ChangeTimerTablesToVarChar(tablePrefix: String))
}
