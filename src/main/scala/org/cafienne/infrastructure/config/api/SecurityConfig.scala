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

class SecurityConfig(val parent: ApiConfig) extends MandatoryConfig {
  def path = "security"

  lazy val oidc = new OIDCConfig(this)

  lazy val identityCacheSize: Int = {
    val key = "identity.cache.size"
    val size = readInt(key, 1000)
    if (size == 0) {
      logger.info("Identity caching is disabled")
    } else {
      logger.info("Running with Identity Cache of size " + size)
    }
    size
  }

  lazy val tokenCacheSize: Int = {
    val key = "token.cache.size"
    val size = readInt(key, 1000)
    if (size == 0) {
      logger.info("Token caching is disabled")
    } else {
      logger.info("Running with User Token Cache of size " + size)
    }
    size
  }
}
