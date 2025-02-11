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

package org.cafienne.infrastructure

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.infrastructure.config.CaseSystemConfig

/**
  * JVM wide configurations and settings
  */
object Cafienne {
  /**
    * Returns the BuildInfo as a string (containing JSON)
    *
    * @return
    */
  lazy val version = new CafienneVersion

  def isPlatformOwner(user: UserIdentity): Boolean = isPlatformOwner(user.id)

  def isPlatformOwner(userId: String): Boolean = config.platform.isPlatformOwner(userId)

  /**
    * Configuration settings of this Cafienne Platform
    */
  lazy val config: CaseSystemConfig = {
    conf.getOrElse(throw new Exception("Cannot read Cafienne.config because a CaseSystem instance has not yet been created"))
  }

  private var conf: Option[CaseSystemConfig] = None

  def setConfig(caseSystemConfig: CaseSystemConfig): Unit = {
    conf = Some(caseSystemConfig)
  }
}
