package org.cafienne.persistence.querydb.query

import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.cmmn.instance.State
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.config.TestConfig
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.util.SystemConfig
import org.cafienne.persistence.querydb.materializer.slick.QueryDBWriter
import org.cafienne.persistence.querydb.query.cmmn.implementations.CaseInstanceQueriesImpl
import org.cafienne.persistence.querydb.query.exception.CaseSearchFailure
import org.cafienne.persistence.querydb.record.{CaseRecord, TaskRecord}
import org.cafienne.persistence.querydb.schema.QueryDB
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration._

class CaseTaskQueriesTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  val persistenceConfig: PersistenceConfig = new SystemConfig(TestConfig.config).cafienne.persistence
  val queryDB: QueryDB = new QueryDB(persistenceConfig, persistenceConfig.queryDB.jdbcConfig)
  val queryDBWriter: QueryDBWriter = queryDB.writer
  val caseInstanceQueries = new CaseInstanceQueriesImpl(queryDB)

  val tenant = "tenant"
  val case33 = "33"
  val case44 = "44"
  val case55 = "55"
  val case44Child = "44-child"

  val testUser: PlatformUser = TestIdentityFactory.createPlatformUser("test", tenant, Set("A", "B"))
  val userWithAandB: PlatformUser = TestIdentityFactory.createPlatformUser("userWithAplusB", tenant, Set("A", "B"))
  val userWithBandC: PlatformUser = TestIdentityFactory.createPlatformUser("userAplusC", tenant, Set("B", "C"))
  val userWithC: PlatformUser = TestIdentityFactory.createPlatformUser("userC", tenant, Set("C"))

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

  it should "filter all tasks with caseInstanceId" in {
    val res = Await.result(caseInstanceQueries.getCaseTasks(case33, userWithAandB), 1.second)
    res.size must be(2)
  }

  it should "filter all tasks with root case instance id" in {
    val res = Await.result(caseInstanceQueries.getCaseTasks(case44, testUser, includeSubCaseTasks = true), 1.second)
    res.size must be(2)
  }

  it should "retrieve a case instance" in {
    val res = Await.result(caseInstanceQueries.getCaseInstance(case55, userWithC), 1.second)
    res.size must be(1)
  }

  it should "User having access to cases having no tasks  with root case instance id" in {
    val res = Await.result(caseInstanceQueries.getCaseTasks(case55, userWithC), 1.second)
    res.size must be(0)
  }

  it should "User not having access to any case filter all tasks with root case instance id" in {
    assertThrows[CaseSearchFailure] {
      Await.result(caseInstanceQueries.getCaseTasks(case44, userWithC, includeSubCaseTasks = true), 1.second)
    }
  }

  it should "filter all tasks with root case id, but without getting all sub case tasks" in {
    val res = Await.result(caseInstanceQueries.getCaseTasks(case44, testUser), 1.second)
    res.size must be(1)
  }
}
