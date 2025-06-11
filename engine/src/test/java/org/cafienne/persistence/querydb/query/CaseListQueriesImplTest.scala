package org.cafienne.persistence.querydb.query

import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.cmmn.instance.State
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.persistence.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.persistence.querydb.materializer.tenant.TenantStorageTransaction
import org.cafienne.persistence.querydb.query.cmmn.filter.CaseFilter
import org.cafienne.persistence.querydb.query.cmmn.implementations.CaseListQueriesImpl
import org.cafienne.persistence.querydb.record.{CaseRecord, CaseTeamUserRecord, PlanItemRecord}

import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class CaseListQueriesImplTest extends QueryTestBaseClass("case-list-queries") {
  private val caselistQueries = new CaseListQueriesImpl(queryDB)

  private val idOfActiveCase = caseId("active")
  private val idOfTerminatedCase = caseId("terminated")
  private val idOfCompletedCase = caseId("completed")
  private val activeCase: CaseRecord = CaseRecord(id = idOfActiveCase, tenant = tenant, rootCaseId = idOfActiveCase, caseName = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val terminatedCase: CaseRecord = CaseRecord(id = idOfTerminatedCase, tenant = tenant, rootCaseId = idOfTerminatedCase, caseName = "ddd EeE fff", state = State.Terminated.name, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val completedCase: CaseRecord = CaseRecord(id = idOfCompletedCase, tenant = tenant, rootCaseId = idOfCompletedCase, caseName = "ddd EeE fff", state = State.Completed.name, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val childCase: CaseRecord = CaseRecord(id = idOfActiveCase + "_child", tenant = tenant, parentCaseId = activeCase.id, rootCaseId = activeCase.id, caseName = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val childCase2: CaseRecord = CaseRecord(id = idOfActiveCase + "_child2", tenant = tenant, parentCaseId = activeCase.id, rootCaseId = activeCase.id, caseName = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val grandChildCase: CaseRecord = CaseRecord(id = idOfActiveCase + "_child_child", tenant = tenant, parentCaseId = childCase.id, rootCaseId = activeCase.id, caseName = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")

  private val planItem1_1: PlanItemRecord = PlanItemRecord(id = UUID.randomUUID().toString, definitionId = "abc", caseInstanceId = idOfActiveCase, tenant = tenant, stageId = "", name = "planitem1-1", index = 0, currentState = "Active",
    historyState = "", transition = "", planItemType = "CasePlan", required = false, repeating = false, lastModified = Instant.now,
    modifiedBy = "user1", createdOn = Instant.now, createdBy = "user1", taskInput = "", taskOutput = "", mappedInput = "", rawOutput = "")

  // private val planItemId1 = UUID.randomUUID().toString
  private val planItem2_1: PlanItemRecord = PlanItemRecord(id = UUID.randomUUID().toString, definitionId = "abc", caseInstanceId = idOfTerminatedCase, tenant = tenant, stageId = "", name = "planitem2-1", index = 0, currentState = "Completed",
    historyState = "", transition = "", planItemType = "CasePlan", required = false, repeating = false, lastModified = Instant.now,
    modifiedBy = "user1", createdOn = Instant.now, createdBy = "user1", taskInput = "", taskOutput = "", mappedInput = "", rawOutput = "")

  private val user: PlatformUser = TestIdentityFactory.createPlatformUser("user1", tenant, Set("A", "B"))

  private val caseTeamMemberRecords: Seq[CaseTeamUserRecord] = Seq(
    TestIdentityFactory.createTeamMember(activeCase.id, tenant, user, ""),
    TestIdentityFactory.createTeamMember(terminatedCase.id, tenant, user, ""),
    TestIdentityFactory.createTeamMember(completedCase.id, tenant, user, ""),
    TestIdentityFactory.createTeamMember(childCase.id, tenant, user, ""),
    TestIdentityFactory.createTeamMember(childCase2.id, tenant, user, ""),
    TestIdentityFactory.createTeamMember(grandChildCase.id, tenant, user, ""),
  )

  override def beforeAll(): Unit = {
    queryDB.initializeDatabaseSchema()
    val caseUpdater: CaseStorageTransaction = queryDBWriter.createCaseTransaction(null)
    val tenantUpdater: TenantStorageTransaction = queryDBWriter.createTenantTransaction(null)
    caseUpdater.upsert(activeCase)
    caseUpdater.upsert(planItem1_1)
    caseUpdater.upsert(terminatedCase)
    caseUpdater.upsert(completedCase)
    caseUpdater.upsert(childCase)
    caseUpdater.upsert(childCase2)
    caseUpdater.upsert(grandChildCase)
    caseUpdater.upsert(planItem2_1)
    caseTeamMemberRecords.foreach(caseUpdater.upsert)
    TestIdentityFactory.asDatabaseRecords(user).foreach(tenantUpdater.upsert)
    caseUpdater.commit()
    tenantUpdater.commit()
  }

  // *******************************************************************************************************************
  // Responses of type Case
  // *******************************************************************************************************************

  "Create a table" should "succeed the second time as well" in {
    queryDB.initializeDatabaseSchema()
  }

  val tenantFilter: CaseFilter = CaseFilter(Some(tenant))

  it should "retrieve all cases" in {
    val res = Await.result(caselistQueries.getCases(user, tenantFilter), 3.seconds)
    res must contain (activeCase)
    res must contain (terminatedCase)
    res must contain (completedCase)

    // TODO: the old test had only below line (instead of above three lines). Sometimes this would make the test fail for unclear reasons (perhaps timing issues causes by other tests running parallelly???)
    //    res must be (Seq(completedCase, terminatedCase, activeCase))
  }

  it should "retrieve cases filtered by definition" in {
    val res = Await.result(caselistQueries.getCases(user, tenantFilter.copy(caseName = Some("eee"))), 3.seconds)
    res must be (Seq(completedCase, terminatedCase))
  }

  it should "retrieve cases filtered by status" in {
    val res = Await.result(caselistQueries.getCases(user, tenantFilter.copy(status = Some("Active"))), 3.seconds)
    res must contain (activeCase)
  }

  it should "retrieve my terminated cases" in {
    val res = Await.result(caselistQueries.getCases(user, tenantFilter.copy(status = Some("Terminated"))), 3.seconds)
    res must be (Seq(terminatedCase))
  }

  it should "retrieve my completed cases" in {
    val res = Await.result(caselistQueries.getCases(user, tenantFilter.copy(status = Some("Completed"))), 3.seconds)
    res must be (Seq(completedCase))
  }

  it should "retrieve cases filtered by root case id" in {
    val res = Await.result(caselistQueries.getCases(user, CaseFilter(rootCaseId = Some(idOfActiveCase))), 3.seconds)
    res must be (Seq(activeCase, childCase, childCase2, grandChildCase))
  }

  // *******************************************************************************************************************
  // Responses of type CaseList
  // *******************************************************************************************************************

//  it should "filter all cases by definition" in {
//    val res = Await.result(caseQueries.getCasesStats(user = user, Some(tenant), from = 0, numOfResults = 10, caseName = Some("ddd EeE fff")), 1.second)
//    res.size must be (1)
//    res.head must be(caseListDDDEEEFFF)
//  }
//
//  it should "filter all cases by status - active" in {
//    val res = Await.result(caseQueries.getCasesStats(user = user, Some(tenant), from = 0, numOfResults = 10, status = Some("Active")), 1.second)
//    res.size must be (1)
//    res.head must be(caseListActive)
//  }
//
//
//  it should "filter all cases by status - terminated" in {
//    val res = Await.result(caseQueries.getCasesStats(user = user, Some(tenant), from = 0, numOfResults = 10, status = Some("Terminated")), 1.second)
//    res.size must be (1)
//    res.head must be(caseListTerminated)
//  }
}
