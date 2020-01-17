package org.cafienne.service.api.cases

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.cafienne.cmmn.instance.State
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.jdbc.ProjectionsDbConfig
import org.cafienne.service.api.projection.slick.SlickRecordsPersistence
import org.cafienne.service.api.writer.TestConfig
import org.cafienne.service.db.migration.Migrate
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, MustMatchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class CaseQueriesImplTest extends TestKit(ActorSystem("testsystem", TestConfig.config)) with FlatSpecLike with MustMatchers with BeforeAndAfterAll with ProjectionsDbConfig {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val caseQueries = new CaseQueriesImpl
  val updater = new SlickRecordsPersistence

  val tenant = "tenant"

  val idOfActiveCase = "active"
  val activeCase = CaseInstance(id = idOfActiveCase, tenant = tenant, rootCaseId = idOfActiveCase, name = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")

  val planItem1_1 = PlanItem(id = UUID.randomUUID().toString, caseInstanceId = idOfActiveCase, tenant = tenant, stageId = "", name = "planitem1-1", index = 0, currentState = "Active",
    historyState = "", transition = "", planItemType = "CasePlan", required = false, repeating = false, lastModified = Instant.now,
    modifiedBy = "user1", createdOn = Instant.now, createdBy = "user1", taskInput = None, taskOutput = None, mappedInput = None, rawOutput = None)

  val idOfTerminatedCase = "terminated"
  val terminatedCase = CaseInstance(id = idOfTerminatedCase, tenant = tenant, rootCaseId = idOfTerminatedCase, name = "ddd EeE fff", state = State.Terminated.name, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")

  val idOfCompletedCase = "completed"
  val completedCase = CaseInstance(id = idOfCompletedCase, tenant = tenant, rootCaseId = idOfCompletedCase, name = "ddd EeE fff", state = State.Completed.name, failures = 0, lastModified = Instant.now, createdOn = Instant.now) //, casefile = "")

//  val planItemId1 = UUID.randomUUID().toString
  val planItem2_1 = PlanItem(id = UUID.randomUUID().toString, caseInstanceId = idOfTerminatedCase, tenant = tenant, stageId = "", name = "planitem2-1", index = 0, currentState = "Completed",
    historyState = "", transition = "", planItemType = "CasePlan", required = false, repeating = false, lastModified = Instant.now,
    modifiedBy = "user1", createdOn = Instant.now, createdBy = "user1", taskInput = None, taskOutput = None, mappedInput = None, rawOutput = None)

  val planItemHistory2_1 = PlanItemHistory(id = UUID.randomUUID().toString, planItemId = planItem2_1.id, caseInstanceId = idOfTerminatedCase, tenant = tenant, stageId = "", name = "planitem2", currentState = "", historyState = "", transition = "", index = 0, planItemType = "", required = false, repeating = false, lastModified = Some(Instant.now),
    modifiedBy = "user1", eventType="?", sequenceNr = 1)

  val caseListActive = CaseList(definition = "aaa bbb ccc", numActive = 1L, numClosed = 0L)
  val caseListDDDEEEFFF = CaseList(definition = "ddd EeE fff", numTerminated = 1L, numCompleted = 1L)
  val caseListTerminated = CaseList(definition = "ddd EeE fff", numTerminated = 1L)

  val user = TestIdentityFactory.createPlatformUser("user1", tenant, List("A", "B"))

  override def beforeAll {
    Migrate.migrateDatabase()
    val records = Seq(activeCase, planItem1_1, terminatedCase, completedCase, planItem2_1, planItemHistory2_1) ++ TestIdentityFactory.asDatabaseRecords(user)
    updater.bulkUpdate(records)
//    Await.ready(Future.sequence(caseQueries.bulkUpdate(records), 1.seconds)
  }

  // *******************************************************************************************************************
  // Responses of type Case
  // *******************************************************************************************************************

  "Create a table" should "succeed the second time as well" in {
    Migrate.migrateDatabase()
  }

  "A query" should "retrieve an existing case" in {
    val res = Await.result(caseQueries.getCaseInstance(activeCase.id, user), 3.seconds)
    res must be (Some(activeCase))
  }

  it should "retrieve all cases" in {
    val res = Await.result(caseQueries.getCases(Some(tenant), from = 0, numOfResults = 10, user = user, definition = None, status = None), 3.seconds)
    res must be (Vector(completedCase, terminatedCase, activeCase))
  }

  it should "retrieve cases filtered by definition" in {
    val res = Await.result(caseQueries.getCases(Some(tenant), from = 0, numOfResults = 10, user = user, definition = Some("eee"), status = None), 3.seconds)
    res must be (Seq(completedCase, terminatedCase))
  }

  it should "retrieve cases filtered by status" in {
    val res = Await.result(caseQueries.getCases(Some(tenant), from = 0, numOfResults = 10, user = user, definition = None, status = Some("Active")), 3.seconds)
    res must be (Seq((activeCase)))
  }

  it should "retrieve my terminated cases" in {
    val res = Await.result(caseQueries.getMyCases(Some(tenant), from = 0, numOfResults = 10, user = user, definition = Some("eee"), status = Some("Terminatme")), 3.seconds)
    res must be (Seq((terminatedCase)))
  }

  it should "retrieve my completed cases" in {
    val res = Await.result(caseQueries.getMyCases(Some(tenant), from = 0, numOfResults = 10, user = user, definition = Some("eee"), status = Some("Completed")), 3.seconds)
    res must be (Seq((completedCase)))
  }

  // *******************************************************************************************************************
  // Responses of type CaseList
  // *******************************************************************************************************************

  it should "filter all cases by definition" in {
    val res = Await.result(caseQueries.getCasesStats(Some(tenant), from = 0, numOfResults = 10, user = user, definition = Some("ddd EeE fff")), 1.second)
    res.size must be (1)
    res.head must be(caseListDDDEEEFFF)
  }

  it should "filter all cases by status - active" in {
    val res = Await.result(caseQueries.getCasesStats(Some(tenant), from = 0, numOfResults = 10, user = user, status = Some("Active")), 1.second)
    res.size must be (1)
    res.head must be(caseListActive)
  }


  it should "filter all cases by status - terminated" in {
    val res = Await.result(caseQueries.getCasesStats(Some(tenant), from = 0, numOfResults = 10, user = user, status = Some("Terminated")), 1.second)
    res.size must be (1)
    res.head must be(caseListTerminated)
  }

  it should "retrieve all planItems" in {
    val res = Await.result(caseQueries.getPlanItems(planItem1_1.caseInstanceId, user), 1.second)
    res.size must be (1)
    res.head must be(planItem1_1)
  }

  it should "retrieve a planItem" in {
    val res = Await.result(caseQueries.getPlanItem(planItem2_1.id, user), 1.second)
    res.size must be (1)
    res.head must be(planItem2_1)
  }

  it should "retrieve planItemHistory records" in {
    val res = Await.result(caseQueries.getPlanItemHistory(planItemHistory2_1.planItemId, user), 1.second)
    res.size must be (1)
    res.head must be(planItemHistory2_1)
  }

}
