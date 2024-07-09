package org.cafienne.querydb.materializer

import org.apache.pekko.persistence.query.Offset
import org.cafienne.querydb.materializer.cases.TestCaseStorageTransaction
import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.materializer.tenant.TenantStorageTransaction

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object TestQueryDB extends QueryDBStorage {

  // Some day: extend this with tests for TenantStorage and ConsentGroupStorage

  val transactions: ListBuffer[TestCaseStorageTransaction] = new ListBuffer[TestCaseStorageTransaction]()

  def latest: TestCaseStorageTransaction = transactions.last

  def hasTransaction(persistenceId: String): Boolean = transactions.exists(_.persistenceId == persistenceId)

  def getTransaction(persistenceId: String): TestQueryDBTransaction = {
    // println(s"Searching for transaction on case $persistenceId in ${transactions.size} transactions, with id's ${transactions.map(_.persistenceId)}")
    transactions.find(_.persistenceId == persistenceId).getOrElse(throw new Exception(s"Cannot find a transaction for case $persistenceId"))
  }

  override def createCaseTransaction(caseInstanceId: String): TestCaseStorageTransaction = {
    // println(s"\n\nAsking for new case transaction on persistence id $caseInstanceId\n\n")
    transactions += new TestCaseStorageTransaction(caseInstanceId)
    transactions.last
  }

  override def createConsentGroupTransaction(groupId: String): ConsentGroupStorageTransaction = ???

  override def createTenantTransaction(tenant: String): TenantStorageTransaction = ???

  override def getOffset(offsetName: String): Future[Offset] = Future.successful(Offset.noOffset)
}
