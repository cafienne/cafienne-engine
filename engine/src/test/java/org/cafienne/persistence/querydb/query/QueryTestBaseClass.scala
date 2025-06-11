package org.cafienne.persistence.querydb.query

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import org.cafienne.infrastructure.config.TestConfig
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.util.SystemConfig
import org.cafienne.persistence.querydb.materializer.slick.QueryDBWriter
import org.cafienne.persistence.querydb.query.cmmn.implementations.CaseInstanceQueriesImpl
import org.cafienne.persistence.querydb.schema.QueryDB
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.must.Matchers

import scala.concurrent.ExecutionContext

class QueryTestBaseClass(testName: String) extends TestKit(ActorSystem(s"testsystem-$testName", TestConfig.config)) with AnyFlatSpecLike with Matchers with BeforeAndAfterAll {
  val persistenceConfig: PersistenceConfig = new SystemConfig(TestConfig.config).cafienne.persistence
  val queryDB: QueryDB = new QueryDB(persistenceConfig, persistenceConfig.queryDB.jdbcConfig)
  val queryDBWriter: QueryDBWriter = queryDB.writer
  val caseInstanceQueries = new CaseInstanceQueriesImpl(queryDB)
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val tenant: String = s"tenant-$testName"

  def caseId(prefix: String): String = s"$prefix-$testName"
}
