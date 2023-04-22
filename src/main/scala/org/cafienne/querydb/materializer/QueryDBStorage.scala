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

package org.cafienne.querydb.materializer

import org.cafienne.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.materializer.tenant.TenantStorageTransaction
import akka.persistence.query.Offset
import org.cafienne.querydb.materializer.board.BoardStorageTransaction

import scala.concurrent.Future

/**
  * Generic trait that can serve as the base for CaseStorage, TenantStorage and ConsentGroupStorage.
  * Currently no shared logic though...
  */
trait QueryDBStorage {

  def createCaseTransaction(caseInstanceId: String): CaseStorageTransaction

  def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction

  def createTenantTransaction(tenant: String): TenantStorageTransaction

  def createBoardTransaction(tenant: String): BoardStorageTransaction

  def getOffset(offsetName: String): Future[Offset]
}
