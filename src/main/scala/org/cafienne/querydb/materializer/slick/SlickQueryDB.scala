package org.cafienne.querydb.materializer.slick

import akka.persistence.query.Offset
import org.cafienne.infrastructure.jdbc.cqrs.JDBCOffsetStorage
import org.cafienne.querydb.materializer.QueryDBStorage
import org.cafienne.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.materializer.tenant.TenantStorageTransaction
import org.cafienne.querydb.schema.QueryDBSchema

import scala.concurrent.{ExecutionContext, Future}

object SlickQueryDB extends QueryDBStorage with QueryDBSchema {

  override def createCaseTransaction(caseInstanceId: String): CaseStorageTransaction = new SlickCaseTransaction

  override def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction = new SlickConsentGroupTransaction

  override def createTenantTransaction(tenant: String): TenantStorageTransaction = new SlickTenantTransaction

  private val databaseConfig = dbConfig

  override def getOffset(offsetName: String): Future[Offset] = new JDBCOffsetStorage {
    override val storageName: String = offsetName
    override lazy val dbConfig = databaseConfig
    override implicit val ec: ExecutionContext = db.ioExecutionContext
  }.getOffset
}
