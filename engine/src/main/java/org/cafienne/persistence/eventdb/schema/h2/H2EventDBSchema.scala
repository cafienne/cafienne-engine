package org.cafienne.persistence.eventdb.schema.h2

import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.flyway.SchemaMigrator

object H2EventDBSchema extends EventDBSchema {
  override def scripts(tablePrefix: String): Seq[SchemaMigrator] = Seq(new V1_1_13__AddTimerService(tablePrefix), new V1_1_16__CreateAkkaSchema(tablePrefix))
}
