package org.cafienne.persistence.eventdb.schema.hsqldb

import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.flyway.SchemaMigrator

object HSQLDBEventDBSchema extends EventDBSchema {
  override def scripts(): Seq[SchemaMigrator] = Seq(
    new V1_1_13__AddTimerService(),
    new V1_1_14__CreateAkkaSchema(),
    new V1_1_15__CreateAkkaSchema(),
    new V1_1_16__CreateAkkaSchema(),
    new V1_1_17__CreateAkkaSchema(),
    new V1_1_18__CreateAkkaSchema(),
    new V1_1_19__CreateAkkaSchema(),
    new V1_1_20__CreateAkkaSchema())
}
