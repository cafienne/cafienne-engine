package org.cafienne.querydb.materializer

import org.cafienne.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.materializer.tenant.TenantStorageTransaction
import akka.persistence.query.Offset

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
