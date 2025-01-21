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

package org.cafienne.persistence.infrastructure.jdbc

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

/**
 * Basic JDBC abstraction on Slick that can be used to hook a database connection
 * based on config properties.
 *
 */
trait SlickDatabaseProfile {

  val dbConfig: DatabaseConfig[JdbcProfile]

  lazy val db: dbConfig.profile.backend.JdbcDatabaseDef = dbConfig.db

  lazy val isSQLServer: Boolean = dbConfig.profile.isInstanceOf[slick.jdbc.SQLServerProfile]
}
