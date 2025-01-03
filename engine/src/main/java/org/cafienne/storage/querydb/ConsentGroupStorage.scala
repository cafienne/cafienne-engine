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

package org.cafienne.storage.querydb

import org.apache.pekko.Done

import scala.concurrent.Future

class ConsentGroupStorage extends QueryDBStorage {

  import dbConfig.profile.api._

  def deleteGroup(groupId: String): Future[Done] = {
    addStatement(TableQuery[ConsentGroupTable].filter(_.id === groupId).delete)
    addStatement(TableQuery[ConsentGroupMemberTable].filter(_.group === groupId).delete)
    commit()
  }
}
