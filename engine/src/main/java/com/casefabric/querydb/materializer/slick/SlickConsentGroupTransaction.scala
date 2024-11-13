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

package com.casefabric.querydb.materializer.slick

import com.casefabric.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import com.casefabric.querydb.record.{ConsentGroupMemberRecord, ConsentGroupRecord}

class SlickConsentGroupTransaction extends SlickQueryDBTransaction with ConsentGroupStorageTransaction {

  import dbConfig.profile.api._

  override def upsert(record: ConsentGroupRecord): Unit = addStatement(TableQuery[ConsentGroupTable].insertOrUpdate(record))

  override def upsert(record: ConsentGroupMemberRecord): Unit = addStatement(TableQuery[ConsentGroupMemberTable].insertOrUpdate(record))

  override def delete(record: ConsentGroupMemberRecord): Unit = addStatement(TableQuery[ConsentGroupMemberTable].filter(_.group === record.group).filter(_.userId === record.userId).filter(_.role === record.role).delete)

  override def deleteConsentGroupMember(groupId: String, userId: String): Unit = addStatement(TableQuery[ConsentGroupMemberTable].filter(_.group === groupId).filter(_.userId === userId).delete)
}
