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

package com.casefabric.infrastructure.config.persistence

import com.typesafe.config.Config
import com.casefabric.infrastructure.config.util.ChildConfigReader


class JDBCConfig(val parent: PersistenceConfig, val systemConfig: Config, val jdbcJournalConfig: Config) extends ChildConfigReader {
  def path = ""

  lazy val useSharedDB: Boolean = jdbcJournalConfig.hasPath("use-shared-db")
  lazy val databaseKey = {
    if (useSharedDB) jdbcJournalConfig.getString("use-shared-db")
    else "slick"
  }

  lazy val dbConfig: Config = if (useSharedDB) {
    systemConfig.getConfig("pekko-persistence-jdbc.shared-databases")
  } else jdbcJournalConfig

  //  println("DB Config: " + jdbcJournalConfig.root().render(ConfigRenderOptions.concise().setFormatted(true)))
//  println("DB Config: " + jdbcJournalConfig.getConfig(s"$databaseKey.db").root().render(ConfigRenderOptions.concise().setFormatted(true)))

  lazy val url: String = dbConfig.getString(s"$databaseKey.db.url")
  lazy val user: Option[String] = if (dbConfig.hasPath(s"$databaseKey.db.user")) Some(dbConfig.getString(s"$databaseKey.db.user")) else None
  lazy val password: Option[String] = if (dbConfig.hasPath(s"$databaseKey.db.password")) Some(dbConfig.getString(s"$databaseKey.db.password")) else None
  lazy val profile: String = dbConfig.getString("slick.profile")

  lazy val isPostgres: Boolean = profile.contains("Postgres")
  lazy val isSQLServer: Boolean = profile.contains("SQLServer")
  lazy val isH2: Boolean = profile.contains("H2")
}
