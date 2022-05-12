package org.cafienne.service.akkahttp.writer

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.testkit.{TestKit, TestProbe}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.event.{CaseDefinitionApplied, CaseModified}
import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition
import org.cafienne.cmmn.test.TestScript
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.json.ValueMap
import org.cafienne.querydb.materializer.cases.CaseEventSink
import org.cafienne.system.CaseSystem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.concurrent.duration._

class CaseFileWriterTest
    extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  private val storeEventsActor = system.actorOf(Props(classOf[CreateEventsInStoreActor]), "storeevents-actor")
  private val tp = TestProbe()

  implicit val logger: LoggingAdapter = Logging(system, getClass)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(5, Millis)))

  private def sendEvent(evt: Any) = {
    within(10.seconds) {
      tp.send(storeEventsActor, evt)
      tp.expectMsg(evt)
    }
  }

  val persistence = new TestQueryDBTransaction()

  val cpw = new CaseEventSink(new CaseSystem(system))
  cpw.start()

  val caseInstanceId = "9fc49257_7d33_41cb_b28a_75e665ee3b2c"
  val user: TenantUser = TestIdentityFactory.createTenantUser("test")
  val caseDefinition: CaseDefinition = TestScript.getCaseDefinition("testdefinition/helloworld.xml")

  val eventFactory = new EventFactory(caseInstanceId, caseDefinition, user)

  val ivm: Instant = Instant.now()
  val caseDefinitionApplied: CaseDefinitionApplied = eventFactory.createCaseDefinitionApplied()
  val path = "Greeting"
  val jsonValue = new ValueMap("Message", "hi there", "From", "admin")
  val caseFileEvent: CaseFileItemTransitioned = eventFactory.createCaseFileEvent(path, jsonValue, CaseFileItemTransition.Create)
  val caseModifiedEvent: CaseModified = eventFactory.createCaseModified(ivm)

  "CaseProjectionsWriter" must {
    "add and update a case file" in {

      sendEvent(caseDefinitionApplied)
      sendEvent(caseFileEvent)
      sendEvent(caseModifiedEvent)

//      val expectedCaseFileContent = """{
//                             |  "Greeting" : {
//                             |    "Message" : "hi there",
//                             |    "From" : "admin"
//                             |  }
//                             |}""".stripMargin
//      import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
//      import org.cafienne.querydb.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, CaseRoleRecord}

//      Thread.sleep(2000)
//      eventually {
//        persistence.records.length shouldBe 6 // Events generate below 6 records
//        persistence.records.count(_.isInstanceOf[CaseDefinitionRecord]) shouldBe 1
//        persistence.records.count(_.isInstanceOf[CaseRoleRecord]) shouldBe 2
//        persistence.records.count(_.isInstanceOf[CaseRecord]) shouldBe 1
//        persistence.records.count(_.isInstanceOf[CaseFileRecord]) shouldBe 1
//        persistence.records.count(_.isInstanceOf[OffsetRecord]) shouldBe 1
//        persistence.records.find(_.isInstanceOf[CaseFileRecord]) match {
//          case Some(cs: CaseFileRecord) => cs.data shouldBe expectedCaseFileContent
//          case other => assert(false, "CaseFile object expected, found " + other.getClass.getName)
//        }
//      }
    }
  }
}
