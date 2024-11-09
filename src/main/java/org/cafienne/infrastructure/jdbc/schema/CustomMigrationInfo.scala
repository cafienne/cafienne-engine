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

package org.cafienne.infrastructure.jdbc.schema

import slick.migration.api.Migration
import slick.migration.api.flyway.MigrationInfo
import slick.migration.api.flyway.MigrationInfo.Provider

/**
  * Due to an earlier bug in slick flyway migration library, description did not give repeated predictable outcome.
  * Therefore in Cafienne we made a CustomMigrationInfo (called MigrationInfoHack) to overcome this problem.
  *
  * Original ticket: https://github.com/nafg/slick-migration-api-flyway/issues/26
  *
  * The bug has been fixed in the library; however, in the new version, the construction of the description is done
  * in a different manner than in the Cafienne version. Hence we need to continue to use our own version.
  * So we have renamed it to CustomMigrationInfo instead of MigrationInfoHack...
  */
object CustomMigrationInfo {
  import slick.migration.api.flyway.MigrationInfo.Provider.{crc32, sql}

  def provider: Provider[Migration] = {
    new Provider[Migration]({ migration =>
      val sqlStrings = sql(migration)

      MigrationInfo(
        description = migration.getClass.getSimpleName, // <- actual override
        script = sqlStrings.mkString("\n"),
        checksum = Some(crc32(sqlStrings).toInt),
        location = migration.getClass.getName
      )
    })
  }
}
