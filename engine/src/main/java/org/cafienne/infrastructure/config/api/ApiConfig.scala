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

package org.cafienne.infrastructure.config.api

import org.cafienne.infrastructure.config.util.MandatoryConfig
import org.cafienne.infrastructure.config.CaseSystemConfig

class ApiConfig(val parent: CaseSystemConfig) extends MandatoryConfig {
  def path = "api"

  lazy val bindHost: String = {
    config.getString("bindhost")
  }

  lazy val bindPort: Int = {
    config.getInt("bindport")
  }

  val anonymousConfig: AnonymousConfig = {
    new AnonymousConfig(this)
  }

  lazy val security: SecurityConfig = new SecurityConfig(this)
}
