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

package org.cafienne.infrastructure.config.persistence.eventdb

import com.typesafe.config.Config
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.util.ChildConfigReader


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
  lazy val user: String = {
    if (dbConfig.hasPath(s"$databaseKey.db.user")) {
      dbConfig.getString(s"$databaseKey.db.user")
    } else {
      null
    }
  }
  lazy val password: String = { // Return null if we have no user, otherwise configured pwd or an empty string
    if (user == null) {
      null
    } else if (dbConfig.hasPath(s"$databaseKey.db.password")) {
      dbConfig.getString(s"$databaseKey.db.password")
    } else {
      ""
    }
  }
  lazy val profile: Profile = Profile.from(dbConfig.getString("slick.profile"))

  lazy val isPostgres: Boolean = profile.isPostgres;
  lazy val isSQLServer: Boolean = profile.isSQLServer
  lazy val isH2: Boolean = profile.isH2
}
