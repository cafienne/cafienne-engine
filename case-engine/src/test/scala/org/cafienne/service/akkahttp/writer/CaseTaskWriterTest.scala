package org.cafienne.service.akkahttp.writer

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.testkit.{TestKit, TestProbe}
import org.cafienne.cmmn.instance.{State, Transition}
import org.cafienne.cmmn.test.TestScript
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.cqrs.OffsetRecord
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.querydb.record._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.concurrent.duration._

class CaseTaskWriterTest
  extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  private val storeEventsActor = system.actorOf(Props(classOf[CreateEventsInStoreActor]), "storeevents-actor")
  private val tp = TestProbe()

  implicit val logger: LoggingAdapter = Logging(system, getClass)

  implicit override val patienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(5, Millis)))

  private def sendEvent(evt: Any) = {
    within(10.seconds) {
      tp.send(storeEventsActor, evt)
      tp.expectMsg(evt)
    }
  }

  val persistence = new TestPersistence()

  val cpw = new CaseEventSink(persistence, NoOffsetStorage)
  cpw.start()


  val caseInstanceId = "9fc49257_7d33_41cb_b28a_75e665ee3b2c"
  val user = TestIdentityFactory.createTenantUser("test")
  val caseDefinition = TestScript.getCaseDefinition("testdefinition/helloworld.xml")

  val eventFactory = new EventFactory(caseInstanceId, caseDefinition, user)
  val ivm = Instant.now

  val caseDefinitionApplied = eventFactory.createCaseDefinitionApplied()
  val caseModifiedEvent = eventFactory.createCaseModified(ivm)
  val planItemTransitioned = eventFactory.createPlanItemTransitioned("1", "HumanTask", State.Terminated, State.Active, Transition.Terminate, ivm)

  "CaseProjectionsWriter" must {
    "add and update tasks" in {

      sendEvent(caseDefinitionApplied)
      sendEvent(planItemTransitioned)
      sendEvent(caseModifiedEvent)

      Thread.sleep(2000)
      eventually {
        println(s"Found ${persistence.records.length} records, of types ${persistence.records.map(_.getClass.getSimpleName).toSet.mkString(",")}")
        persistence.records.length shouldBe 7
        persistence.records.count(_.isInstanceOf[CaseDefinitionRecord]) shouldBe 1
        persistence.records.count(_.isInstanceOf[CaseRoleRecord]) shouldBe 2
        persistence.records.count(_.isInstanceOf[CaseRecord]) shouldBe 1
        persistence.records.count(_.isInstanceOf[CaseFileRecord]) shouldBe 1
        persistence.records.count(_.isInstanceOf[PlanItemHistoryRecord]) shouldBe 1  // so ... then why is this called CaseTaskWriterTest???
        persistence.records.count(_.isInstanceOf[OffsetRecord]) shouldBe 1
      }
    }
  }
}
