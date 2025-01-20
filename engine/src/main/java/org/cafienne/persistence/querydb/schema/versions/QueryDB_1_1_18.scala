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

import org.cafienne.persistence.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.persistence.querydb.schema.QueryDBSchema
import org.cafienne.persistence.querydb.schema.table.TenantTables
import slick.migration.api.TableMigration

object QueryDB_1_1_18 extends DbSchemaVersion with QueryDBSchema
  with TenantTables {

  val version = "1.1.18"
  val migrations = addUserRoleTenantIndex

  import dbConfig.profile.api._

  def addUserRoleTenantIndex = TableMigration(TableQuery[UserRoleTable]).addIndexes(_.indexUserRoleTenant)

}
