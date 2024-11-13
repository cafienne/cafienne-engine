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

package com.casefabric.querydb.materializer.consentgroup

import com.casefabric.querydb.materializer.QueryDBTransaction
import com.casefabric.querydb.record.{ConsentGroupMemberRecord, ConsentGroupRecord}

trait ConsentGroupStorageTransaction extends QueryDBTransaction {
  def upsert(record: ConsentGroupRecord): Unit

  def upsert(record: ConsentGroupMemberRecord): Unit

  def delete(record: ConsentGroupMemberRecord): Unit

  def deleteConsentGroupMember(groupId: String, userId: String): Unit = ???
}
