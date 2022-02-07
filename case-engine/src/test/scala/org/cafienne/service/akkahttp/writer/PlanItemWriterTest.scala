package org.cafienne.service.akkahttp.writer

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.testkit.{TestKit, TestProbe}
import org.cafienne.cmmn.test.TestScript
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.querydb.record.{CaseRecord, PlanItemRecord}
import org.cafienne.querydb.schema.QueryDB
import org.cafienne.system.CaseSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.concurrent.duration._

class PlanItemWriterTest
  extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  //Ensure the database is setup completely (including the offset store)
  QueryDB.verifyConnectivity()

  private val storeEventsActor = system.actorOf(Props(classOf[CreateEventsInStoreActor]), "storeevents-actor")
  private val tp = TestProbe()

  implicit val logger: LoggingAdapter = Logging(system, getClass)

  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(5, Millis)))

  private def sendEvent(evt: Any) = {
    within(5.seconds) {
      tp.send(storeEventsActor, evt)
      tp.expectMsg(evt)
    }
  }

  val persistence = new TestPersistence()

  val cpw = new CaseEventSink(new CaseSystem)
  cpw.start()


  val caseInstanceId = "c140aae8_dd10_4ece_8fb1_5f7a199e49e7"
  val user = TestIdentityFactory.createTenantUser("test")
  val caseDefinition = TestScript.getCaseDefinition("testdefinition/helloworld.xml")

  val eventFactory = new EventFactory(caseInstanceId, caseDefinition, user)
  val ivm = Instant.now

  val caseDefinitionApplied = eventFactory.createCaseDefinitionApplied()
  val caseModifiedEvent = eventFactory.createCaseModified(ivm)
  val planItemCreated = eventFactory.createPlanItemCreated("1", "CasePlan", "HelloWorld", "", ivm)

  "CaseProjectionsWriter" must {
    "add and update plan items" in {

      sendEvent(caseDefinitionApplied)
      sendEvent(planItemCreated)
      sendEvent(caseModifiedEvent)

      Thread.sleep(1000)
      eventually {
        persistence.records.length shouldBe 8
        assert(persistence.records.exists(x => x.isInstanceOf[CaseRecord]))
        assert(persistence.records.exists(x => x.isInstanceOf[PlanItemRecord]))
      }
    }
  }
}
