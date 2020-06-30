package org.cafienne.service.api.tasks

import java.time.Instant

import org.cafienne.cmmn.instance.State
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.jdbc.QueryDbConfig
import org.cafienne.service.api.Sort
import org.cafienne.service.api.cases.table.{CaseRecord, CaseTeamMemberRecord}
import org.cafienne.service.api.projection.TaskSearchFailure
import org.cafienne.service.api.projection.slick.SlickRecordsPersistence
import org.cafienne.service.db.migration.Migrate
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class TaskQueriesImplTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll with QueryDbConfig {

  val taskQueries = new TaskQueriesImpl
  val updater = new SlickRecordsPersistence

  val tenant = "tenant"
  val case33 = "33"
  val case44 = "44"

  val testUser = TestIdentityFactory.createPlatformUser("test", tenant, List("A", "B"))
  val userWithAandB = TestIdentityFactory.createPlatformUser("userWithAplusB", tenant, List("A", "B"))
  val userWithBandC = TestIdentityFactory.createPlatformUser("userAplusC", tenant, List("B", "C"))

  override def beforeAll {
    Migrate.migrateDatabase()

    def freshData = Seq(
      TaskRecord( "1", case33, tenant = tenant, role = "A", owner = "Piet", taskState = "Unassigned", createdOn = Instant.now, lastModified = Instant.now),
      TaskRecord( "2", case33, tenant = tenant, role = "A", owner = "Jan", createdOn = Instant.now, lastModified = Instant.now),
      TaskRecord( "3", case44, tenant = tenant, role = "B", owner = "Aart", createdOn = Instant.now, lastModified = Instant.now),
    ) ++ TestIdentityFactory.asDatabaseRecords(Seq(testUser, userWithAandB, userWithBandC))

    println("Writing cases")

    Await.ready(updater.bulkUpdate({
      Seq(
        CaseRecord(id = case33, tenant = tenant, rootCaseId = case33, name = "aaa bbb ccc", state = State.Failed.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now),
        CaseRecord(id = case44, tenant = tenant, rootCaseId = case44, name = "aaa bbb ccc", state = State.Failed.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now)
      )
    }), 2.seconds)

    println("Writing case team members")

    Await.ready(updater.bulkUpdate({
      Seq(
        CaseTeamMemberRecord(caseInstanceId = case33, tenant = tenant, memberId = testUser.userId, caseRole = "", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case33, tenant = tenant, memberId = testUser.userId, caseRole = "A", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case33, tenant = tenant, memberId = testUser.userId, caseRole = "B", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case33, tenant = tenant, memberId = userWithAandB.userId, caseRole = "", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case33, tenant = tenant, memberId = userWithAandB.userId, caseRole = "A", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case33, tenant = tenant, memberId = userWithAandB.userId, caseRole = "B", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case44, tenant = tenant, memberId = testUser.userId, caseRole = "", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case44, tenant = tenant, memberId = testUser.userId, caseRole = "A", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case44, tenant = tenant, memberId = testUser.userId, caseRole = "B", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case44, tenant = tenant, memberId = userWithAandB.userId, caseRole = "", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case44, tenant = tenant, memberId = userWithAandB.userId, caseRole = "A", isTenantUser = true, isOwner = true, active = true),
        CaseTeamMemberRecord(caseInstanceId = case44, tenant = tenant, memberId = userWithAandB.userId, caseRole = "B", isTenantUser = true, isOwner = true, active = true),
      )
    }), 2.seconds)

    println("Writing tasks and tenant users")
    Await.ready(updater.bulkUpdate(freshData), 2.seconds)
  }

  "Create a table" should "succeed the second time as well" in {
    Migrate.migrateDatabase()
  }

  "A query" should "give a search failure when task not found" in {
    assertThrows[TaskSearchFailure] {
      Await.result(taskQueries.getTask("123", testUser), 3.seconds)
    }
  }

  it should "get an existing task" in {
    val res = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    res.caseInstanceId must be (case33)
  }

  it should "retrieve a caseinstanceId by taskId" in {
    val res = Await.result(taskQueries.authorizeTaskAccessAndReturnCaseAndTenantId("1", testUser), 1.second)
    res must be (case33, tenant)
  }

  it should "retrieve nothing by unknown taskId" in {
    assertThrows[TaskSearchFailure] {
      Await.result(taskQueries.authorizeTaskAccessAndReturnCaseAndTenantId("10", testUser), 1.second)
    }
  }

  it should "filter all tasks" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, None, 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
  }

  it should "filter all tasks with caseInstanceId" in {
    val res = Await.result(taskQueries.getCaseTasks(case33, userWithAandB), 1.second)
    res.size must be (2)
  }

  it should "not find tasks when not in case team" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, None, 0, 100, userWithBandC, None), 1.second)
    res.size must be (0)
  }

  it should "filter all tasks with pagination" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, None, 0, 2, userWithAandB, None), 1.second)
    res.size must be (2)
  }

  it should "filter all tasks with pagination, second page" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, None, 1, 100, userWithAandB, None), 1.second)
    res.size must be (2)
  }

  it should "insertion order correctly when not sorting" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, Some(Sort("owner", None)), 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
    res.head.id must be ("1")
    res.last.id must be ("3")
  }

  it should "order correctly by non default column in desc direction" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, Some(Sort("owner", Some("desc"))), 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
    res.map(record => record.owner) must be (Seq("Piet", "Jan", "Aart"))
    res.head.id must be ("1")
    res.last.id must be ("3")

  }

  it should "order correctly by non default column in asc direction" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, Some(Sort("owner", Some("ASC"))), 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
    res.map(record => record.owner) must be (Seq("Aart", "Jan", "Piet"))
    res.head.id must be ("3")
    res.last.id must be ("1")
  }

  it should "get task count" in {
    val res = Await.result(taskQueries.getCountForUser(userWithAandB, Some(tenant)), 1.second)
    res.claimed must be (0)
    res.unclaimed must be (3)
  }

  it should "update a task" in {
    val current = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    val freshTask = current.copy(taskState = "Assigned")
    Await.ready(updater.bulkUpdate(Seq(freshTask)), 3.seconds)
    val res = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    res.id must be ("1")
    res.taskState must be ("Assigned")
  }
}
