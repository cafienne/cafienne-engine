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

package com.casefabric.querydb.materializer

import com.casefabric.querydb.materializer.cases.CaseStorageTransaction
import com.casefabric.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import com.casefabric.querydb.materializer.tenant.TenantStorageTransaction
import org.apache.pekko.persistence.query.Offset

import scala.concurrent.Future

/**
  * Generic trait that can serve as the base for CaseStorage, TenantStorage and ConsentGroupStorage.
  * Currently no shared logic though...
  */
trait QueryDBStorage {

  def createCaseTransaction(caseInstanceId: String): CaseStorageTransaction

  def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction

  def createTenantTransaction(tenant: String): TenantStorageTransaction

  def getOffset(offsetName: String): Future[Offset]
}
