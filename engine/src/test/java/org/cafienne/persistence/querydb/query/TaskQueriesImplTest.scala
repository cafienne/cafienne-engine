package org.cafienne.persistence.querydb.query

import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.cmmn.instance.State
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.cmmn.authorization.AuthorizationQueriesImpl
import org.cafienne.persistence.querydb.query.cmmn.implementations.TaskQueriesImpl
import org.cafienne.persistence.querydb.query.exception.TaskSearchFailure
import org.cafienne.persistence.querydb.record.{CaseRecord, TaskRecord}

import java.time.Instant
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContextExecutor}

class TaskQueriesImplTest extends QueryTestBaseClass("task-list-queries") {
  val taskQueries = new TaskQueriesImpl(queryDB)
  val authorizationQueries = new AuthorizationQueriesImpl(queryDB)

  private val case33 = caseId("33")
  private val case44 = caseId("44")
  private val case55 = caseId("55")
  private val case44Child = caseId("44-child")

  private val testUser: PlatformUser = TestIdentityFactory.createPlatformUser("test", tenant, Set("A", "B"))
  private val userWithAandB: PlatformUser = TestIdentityFactory.createPlatformUser("userWithAplusB", tenant, Set("A", "B"))
  private val userWithBandC: PlatformUser = TestIdentityFactory.createPlatformUser("userAplusC", tenant, Set("B", "C"))
  private val userWithC: PlatformUser = TestIdentityFactory.createPlatformUser("userC", tenant, Set("C"))

  override def beforeAll(): Unit = {

    val caseUpdater = queryDBWriter.createCaseTransaction(null)
    val tenantUpdater = queryDBWriter.createTenantTransaction(null)

    queryDB.initializeDatabaseSchema()

    println("Writing cases")
    caseUpdater.upsert(CaseRecord(id = case33, tenant = tenant, rootCaseId = case33, caseName = "aaa bbb ccc", state = State.Failed.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now))
    caseUpdater.upsert(CaseRecord(id = case44, tenant = tenant, rootCaseId = case44, caseName = "aaa bbb ccc", state = State.Failed.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now))
    caseUpdater.upsert(CaseRecord(id = case44Child, tenant = tenant, parentCaseId = case44, rootCaseId = case44, caseName = "aaa bbb ccc", state = State.Failed.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now))
    caseUpdater.upsert(CaseRecord(id = case55, tenant = tenant, rootCaseId = case55, caseName = "aaa bbb ccc", state = State.Active.toString, failures = 0, lastModified = Instant.now, createdOn = Instant.now))
    caseUpdater.commit()

    println("Writing case team members")
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case33, tenant, testUser, ""))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case33, tenant, testUser, "A"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case33, tenant, testUser, "B"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case33, tenant, userWithAandB, ""))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case33, tenant, userWithAandB, "A"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case33, tenant, userWithAandB, "B"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44, tenant, testUser, ""))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44, tenant, testUser, "A"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44, tenant, testUser, "B"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44, tenant, userWithAandB, ""))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44, tenant, userWithAandB, "A"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44, tenant, userWithAandB, "B"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case44Child, tenant, testUser, "B"))
    caseUpdater.upsert(TestIdentityFactory.createTeamMember(case55, tenant, userWithC, "A"))
    caseUpdater.commit()

    println("Writing tasks and tenant users")
    caseUpdater.upsert(TaskRecord("1", case33, tenant = tenant, role = "A", owner = "Jan", createdOn = Instant.now, lastModified = Instant.now))
    caseUpdater.upsert(TaskRecord("2", case33, tenant = tenant, role = "A", owner = "Piet", taskState = "Unassigned", createdOn = Instant.now, lastModified = Instant.now))
    caseUpdater.upsert(TaskRecord("3", case44, tenant = tenant, role = "B", owner = "Aart", createdOn = Instant.now, lastModified = Instant.now))
    caseUpdater.upsert(TaskRecord("4", case44Child, tenant = tenant, role = "B", owner = "Gerrit", createdOn = Instant.now, lastModified = Instant.now))
    TestIdentityFactory.asDatabaseRecords(Seq(testUser, userWithAandB, userWithBandC)).foreach(user => tenantUpdater.upsert(user))

    caseUpdater.commit()
    tenantUpdater.commit()

  }

  "Create a table" should "succeed the second time as well" in {
    queryDB.initializeDatabaseSchema()
  }

  "A query" should "give a search failure when task not found" in {
    assertThrows[TaskSearchFailure] {
      Await.result(taskQueries.getTask("123", testUser), 3.seconds)
    }
  }

  it should "get an existing task" in {
    val res = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    res.caseInstanceId must be(case33)
  }

  it should "retrieve a caseInstanceId and tenant by taskId" in {
    val res = Await.result({
      implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
      authorizationQueries.getCaseMembershipForTask("1", testUser).map(m => (m.caseInstanceId, m.tenant))
    }, 1.second)
    res must be((case33, tenant))
  }

  it should "retrieve nothing by unknown taskId" in {
    assertThrows[TaskSearchFailure] {
      Await.result(authorizationQueries.getCaseMembershipForTask("10", testUser), 1.second)
    }
  }

  it should "filter all tasks" in {
    val res = Await.result(taskQueries.getAllTasks(userWithAandB), 1.second)
    res.size must be(3)
  }

  it should "not find tasks when not in case team" in {
    val res = Await.result(taskQueries.getAllTasks(userWithBandC), 1.second)
    res.size must be(0)
  }

  it should "filter all tasks with pagination" in {
    val res = Await.result(taskQueries.getAllTasks(userWithAandB, area = Area(0, 2)), 1.second)
    res.size must be(2)
  }

  it should "filter all tasks with pagination, second page" in {
    val res = Await.result(taskQueries.getAllTasks(userWithAandB, area = Area(1, 100)), 1.second)
    res.size must be(2)
  }

  it should "insertion order correctly when not sorting" in {
    val res = Await.result(taskQueries.getAllTasks(userWithAandB, sort = Sort(None)), 1.second)
    res.size must be(3)
    res.map(record => record.owner) must be(Seq("Jan", "Piet", "Aart"))
    res.head.id must be("1")
    res.last.id must be("3")
  }

  it should "order correctly by non default column in desc direction" in {
    val res = Await.result(taskQueries.getAllTasks(userWithAandB, sort = Sort.on("owner")), 1.second)
    res.size must be(3)
    res.map(record => record.owner) must be(Seq("Piet", "Jan", "Aart"))
    res.head.id must be("2")
    res.last.id must be("3")

  }

  it should "order correctly by non default column in asc direction" in {
    val res = Await.result(taskQueries.getAllTasks(userWithAandB, sort = Sort.asc("owner")), 1.second)
    res.size must be(3)
    res.map(record => record.owner) must be(Seq("Aart", "Jan", "Piet"))
    res.head.id must be("3")
    res.last.id must be("2")
  }

  it should "get task count" in {
    val res = Await.result(taskQueries.getCountForUser(userWithAandB, Some(tenant)), 1.second)
    res.claimed must be(0)
    res.unclaimed must be(3)
  }

  it should "update a task" in {
    val current = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    val freshTask = current.copy(taskState = "Assigned")
    val caseUpdater = queryDBWriter.createCaseTransaction(null)
    caseUpdater.upsert(freshTask)
    caseUpdater.commit()

    val res = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    res.id must be("1")
    res.taskState must be("Assigned")
  }
}
