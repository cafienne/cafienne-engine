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

package com.casefabric.querydb.schema.versions

import com.casefabric.infrastructure.jdbc.schema.DbSchemaVersion
import com.casefabric.querydb.schema.QueryDBSchema
import com.casefabric.querydb.schema.table.TenantTables
import slick.migration.api.TableMigration

object QueryDB_1_1_18 extends DbSchemaVersion with QueryDBSchema
  with TenantTables {

  val version = "1.1.18"
  val migrations = addUserRoleTenantIndex

  import dbConfig.profile.api._

  def addUserRoleTenantIndex = TableMigration(TableQuery[UserRoleTable]).addIndexes(_.indexUserRoleTenant)

}
