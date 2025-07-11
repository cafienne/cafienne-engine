package org.cafienne.persistence.querydb.query

import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.engine.cmmn.instance.State
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.persistence.querydb.materializer.cases.CaseStorageTransaction
import org.cafienne.persistence.querydb.materializer.tenant.TenantStorageTransaction
import org.cafienne.persistence.querydb.query.result.CaseList
import org.cafienne.persistence.querydb.record.{CaseRecord, CaseTeamUserRecord, PlanItemRecord}

import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class CaseInstanceQueriesImplTest extends QueryTestBaseClass("case-instance-queries") {

  private val idOfActiveCase = caseId("active")
  private val idOfTerminatedCase = caseId("terminated")
  private val idOfCompletedCase = caseId("completed")
  private val activeCase: CaseRecord = CaseRecord(id = idOfActiveCase, tenant = tenant, rootCaseId = idOfActiveCase, caseName = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val terminatedCase: CaseRecord = CaseRecord(id = idOfTerminatedCase, tenant = tenant, rootCaseId = idOfTerminatedCase, caseName = "ddd EeE fff", state = State.Terminated.name, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")
  private val completedCase: CaseRecord = CaseRecord(id = idOfCompletedCase, tenant = tenant, rootCaseId = idOfCompletedCase, caseName = "ddd EeE fff", state = State.Completed.name, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")

  private val planItem1_1: PlanItemRecord = PlanItemRecord(id = UUID.randomUUID().toString, definitionId = "abc", caseInstanceId = idOfActiveCase, tenant = tenant, stageId = "", name = "planitem1-1", index = 0, currentState = "Active",
    historyState = "", transition = "", planItemType = "CasePlan", required = false, repeating = false, lastModified = Instant.now,
    modifiedBy = "user1", createdOn = Instant.now, createdBy = "user1", taskInput = "", taskOutput = "", mappedInput = "", rawOutput = "")

  //  val planItemId1 = UUID.randomUUID().toString
  val planItem2_1: PlanItemRecord = PlanItemRecord(id = UUID.randomUUID().toString, definitionId = "abc", caseInstanceId = idOfTerminatedCase, tenant = tenant, stageId = "", name = "planitem2-1", index = 0, currentState = "Completed",
    historyState = "", transition = "", planItemType = "CasePlan", required = false, repeating = false, lastModified = Instant.now,
    modifiedBy = "user1", createdOn = Instant.now, createdBy = "user1", taskInput = "", taskOutput = "", mappedInput = "", rawOutput = "")

  val caseListActive: CaseList = CaseList(caseName = "aaa bbb ccc", numActive = 1L, numClosed = 0L)
  val caseListDDDEEEFFF: CaseList = CaseList(caseName = "ddd EeE fff", numTerminated = 1L, numCompleted = 1L)
  val caseListTerminated: CaseList = CaseList(caseName = "ddd EeE fff", numTerminated = 1L)

  val user: PlatformUser = TestIdentityFactory.createPlatformUser("user1", tenant, Set("A", "B"))

  val caseTeamMemberRecords: Seq[CaseTeamUserRecord] = Seq(
    TestIdentityFactory.createTeamMember(idOfActiveCase, tenant, user, ""),
    TestIdentityFactory.createTeamMember(idOfTerminatedCase, tenant, user, ""),
    TestIdentityFactory.createTeamMember(idOfCompletedCase, tenant, user, ""),
  )

  override def beforeAll(): Unit = {
    queryDB.initializeDatabaseSchema()
    val caseUpdater: CaseStorageTransaction = queryDBWriter.createCaseTransaction(null)
    val tenantUpdater: TenantStorageTransaction = queryDBWriter.createTenantTransaction(null)
    caseUpdater.upsert(activeCase)
    caseUpdater.upsert(planItem1_1)
    caseUpdater.upsert(terminatedCase)
    caseUpdater.upsert(completedCase)
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

  "A query" should "retrieve an existing case" in {
    val res = Await.result(caseInstanceQueries.getCaseInstance(activeCase.id, user), 3.seconds)
    res must be (Some(activeCase))
  }

  it should "retrieve all planItems" in {
    val res = Await.result(caseInstanceQueries.getPlanItems(planItem1_1.caseInstanceId, user), 1.second)
    res.size must be (1)
    res.head must be(planItem1_1)
  }

  it should "retrieve a planItem" in {
    val res = Await.result(caseInstanceQueries.getPlanItem(planItem2_1.id, user), 1.second)
    res must be(planItem2_1)
  }
}
