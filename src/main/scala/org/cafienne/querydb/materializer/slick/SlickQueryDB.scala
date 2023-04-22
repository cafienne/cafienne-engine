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

package org.cafienne.querydb.materializer.slick

import akka.persistence.query.Offset
import org.cafienne.infrastructure.jdbc.cqrs.JDBCOffsetStorage
import org.cafienne.querydb.materializer.QueryDBStorage
import org.cafienne.querydb.materializer.board.BoardStorageTransaction
import org.cafienne.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.materializer.tenant.TenantStorageTransaction
import org.cafienne.querydb.schema.QueryDBSchema

import scala.concurrent.{ExecutionContext, Future}

object SlickQueryDB extends QueryDBStorage with QueryDBSchema {

  override def createCaseTransaction(caseInstanceId: String): CaseStorageTransaction = new SlickCaseTransaction

  override def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction = new SlickConsentGroupTransaction

  override def createTenantTransaction(tenant: String): TenantStorageTransaction = new SlickTenantTransaction

  override def createBoardTransaction(groupId: String): BoardStorageTransaction = new SlickBoardTransaction

  private val databaseConfig = dbConfig

  override def getOffset(offsetName: String): Future[Offset] = new JDBCOffsetStorage {
    override val storageName: String = offsetName
    override lazy val dbConfig = databaseConfig
    override implicit val ec: ExecutionContext = db.ioExecutionContext
  }.getOffset
}
