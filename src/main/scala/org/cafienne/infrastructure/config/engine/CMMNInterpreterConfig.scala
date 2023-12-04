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

package org.cafienne.infrastructure.config.engine

import org.cafienne.infrastructure.config.util.ChildConfigReader

class CMMNInterpreterConfig(val parent: EngineConfig) extends ChildConfigReader {
  def path = "interpreter"

  val usePureCMMNFaultHandling: Boolean = {
    val setting = readBoolean("cmmn-fault-handling", default = false)
    if (setting) {
      logger.warn("Engine runs in pure CMMN fault handling mode. Note that CMMN fault handling must be explicitly modeled in your case definitions")
    }

    setting
  }
}
