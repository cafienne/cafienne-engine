/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.schema.versions

import org.cafienne.infrastructure.Cafienne
import org.cafienne.persistence.infrastructure.jdbc.schema.QueryDBSchemaVersion
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.migration.api.{Migration, SqlMigration}

class QueryDB_1_1_5(val dbConfig: DatabaseConfig[JdbcProfile])
  extends QueryDBSchemaVersion {
  val version = "1.1.5"
  val migrations: Migration = SqlMigration(s"""DELETE FROM "${Cafienne.config.persistence.tablePrefix}offset_storage" where "name" = 'CaseProjectionsWriter' """)
}
