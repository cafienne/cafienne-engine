package org.cafienne.querydb.materializer.slick

import org.cafienne.querydb.materializer.QueryDBStorage
import org.cafienne.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.materializer.tenant.TenantStorageTransaction

object SlickQueryDB extends QueryDBStorage {

  override def createCaseTransaction(caseInstanceId: String): CaseStorageTransaction = new SlickCaseTransaction

  override def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction = new SlickConsentGroupTransaction

  override def createTenantTransaction(tenant: String): TenantStorageTransaction = new SlickTenantTransaction
}
