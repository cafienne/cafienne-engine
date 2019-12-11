package org.cafienne.service.api.tasks

import java.sql.Timestamp
import java.time.Instant
import java.util.Date

import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.jdbc.DbConfig
import org.cafienne.service.api.Sort
import org.cafienne.service.api.projection.slick.SlickRecordsPersistence
import org.cafienne.service.db.migration.Migrate
import org.scalatest.{BeforeAndAfterAll, FlatSpec, MustMatchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class TaskQueriesImplTest extends FlatSpec with MustMatchers with BeforeAndAfterAll with DbConfig {

  val taskQueries = new TaskQueriesImpl
  val updater = new SlickRecordsPersistence

  val tenant = "tenant"
  val testUser = TestIdentityFactory.createPlatformUser("test", tenant, List("A", "B"))
  val userWithAandB = TestIdentityFactory.createPlatformUser("bla", tenant, List("A", "B"))
  val userWithBandC = TestIdentityFactory.createPlatformUser("bla", tenant, List("B", "C"))


  override def beforeAll {
    Migrate.migrateDatabase()

    def freshData = Seq(
      Task( "1", "33", tenant = tenant, role = "A", owner = "Piet", taskState = "Unassigned", createdOn = Instant.now, lastModified = Instant.now),
      Task( "2", "33", tenant = tenant, role = "A", owner = "Jan", createdOn = Instant.now, lastModified = Instant.now),
      Task( "3", "44", tenant = tenant, role = "B", owner = "Aart", createdOn = Instant.now, lastModified = Instant.now)
    ) ++ TestIdentityFactory.asDatabaseRecords(Seq(testUser, userWithAandB, userWithBandC))

    Await.ready(updater.bulkUpdate(freshData), 2.seconds)
  }

  "Create a table" should "succeed the second time as well" in {
    Migrate.migrateDatabase()
  }

  "A query" should "give a search failure when task not found" in {
    assertThrows[SearchFailure] {
      Await.result(taskQueries.getTask("123", testUser), 3.seconds)
    }
  }

  it should "get an existing task" in {
    val res = Await.result(taskQueries.getTask("1", testUser), 3.seconds)
    res.caseInstanceId must be ("33")
  }

//  it should "retrieve a caseinstanceId by taskId" in {
//    val res = Await.result(taskQueries.getCaseInstanceId("1", user), 1.second)
//    res must be (Some("33"))
//  }
//
//  it should "retrieve nothing by unknown taskId" in {
//    val res = Await.result(taskQueries.getCaseInstanceId("10", user), 1.second)
//    res must be (None)
//  }
//
  it should "filter all tasks" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
  }

  it should "filter all tasks with caseInstanceId" in {
    val res = Await.result(taskQueries.getAllTasks(Some("33"), None, None, None, None, None, None, None, None, 0, 100, userWithAandB, None), 1.second)
    res.size must be (2)
  }

  it should "filter all tasks for role" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, 0, 100, userWithBandC, None), 1.second)
    res.size must be (1)
  }

  it should "filter all tasks with pagination" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, 0, 2, userWithAandB, None), 1.second)
    res.size must be (2)
  }

  it should "filter all tasks with pagination, second page" in {
    // THIS TEST CURRENTLY FAILS BECAUSE THE TASK QUERY CROSS-TENANT DOES NOT DE_DUPLICATE IN THE QUERY BUT IN THE CODE :(
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, None, 1, 100, userWithAandB, None), 1.second)
    res.size must be (2)
  }

  it should "order correctly by non default column" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, Some(Sort("owner", None)), 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
    res.head.id must be ("1")
    res.last.id must be ("3")
  }

  it should "order correctly by non default column in desc direction" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, Some(Sort("owner", Some("desc"))), 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
    res.head.id must be ("1")
    res.last.id must be ("3")

  }

  it should "order correctly by non default column in asc direction" in {
    val res = Await.result(taskQueries.getAllTasks(None, None, None, None, None, None, None, None, Some(Sort("owner", Some("ASC"))), 0, 100, userWithAandB, None), 1.second)
    res.size must be (3)
    res.head.id must be ("3")
    res.last.id must be ("1")
  }

  it should "get task count" in {
    val res = Await.result(taskQueries.getCountForUser(userWithAandB, Some(tenant)), 1.second)
    res.claimed must be (0)
    res.unclaimed must be (1)
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
