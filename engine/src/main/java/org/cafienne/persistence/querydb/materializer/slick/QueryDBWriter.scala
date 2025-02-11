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

package org.cafienne.persistence.querydb.materializer.slick

import org.apache.pekko.persistence.query.Offset
import org.cafienne.persistence.infrastructure.jdbc.cqrs.JDBCOffsetStorage
import org.cafienne.persistence.querydb.materializer.QueryDBStorage
import org.cafienne.persistence.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.persistence.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.persistence.querydb.materializer.tenant.TenantStorageTransaction
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.{ExecutionContext, Future}

class QueryDBWriter(val queryDB: QueryDB) extends QueryDBStorage {

  override def createCaseTransaction(caseInstanceId: String): CaseStorageTransaction = new SlickCaseTransaction(this)

  override def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction = new SlickConsentGroupTransaction(this)

  override def createTenantTransaction(tenant: String): TenantStorageTransaction = new SlickTenantTransaction(this)

  override def getOffset(offsetName: String): Future[Offset] = new JDBCOffsetStorage {
    override val tablePrefix: String = queryDB.tablePrefix
    override val storageName: String = offsetName
    override lazy val dbConfig = queryDB.dbConfig
    override implicit val ec: ExecutionContext = db.ioExecutionContext
  }.getOffset
}
