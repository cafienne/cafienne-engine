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

package com.casefabric.infrastructure

import com.casefabric.actormodel.identity.UserIdentity
import com.casefabric.infrastructure.config.CaseFabricConfig

/**
  * JVM wide configurations and settings
  */
object CaseFabric {

  /**
    * Configuration settings of this CaseFabric Platform
    */
  lazy val config = new CaseFabricConfig

  /**
    * Returns the BuildInfo as a string (containing JSON)
    *
    * @return
    */
  lazy val version = new CaseFabricVersion

  def isPlatformOwner(user: UserIdentity): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = {
    config.platform.isPlatformOwner(userId)
  }
}
