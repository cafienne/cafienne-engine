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

import com.casefabric.BuildInfo
import com.casefabric.json.{JSONReader, Value, ValueMap}

// Note: json creation is somewhat cumbersome, but it is required in order to have the comparison mechanism work properly.
class CaseFabricVersion(val json: ValueMap = JSONReader.parse(Value.convert(BuildInfo.toMap).asMap.toString).asInstanceOf[ValueMap]) {
  /**
    * Returns true if the two versions differ, false if they are the same.
    *
    * @param otherVersionInstance
    * @return
    */
  def differs(otherVersionInstance: CaseFabricVersion): Boolean = {
    !json.equals(otherVersionInstance.json)
  }

  override def toString: String = json.toString
}
