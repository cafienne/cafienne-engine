package org.cafienne.service.api.writer

import java.time.Instant

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.testkit.{TestKit, TestProbe}
import org.cafienne.cmmn.instance.CaseFileItemTransition
import org.cafienne.cmmn.instance.casefile.ValueMap
import org.cafienne.cmmn.test.TestScript
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.jdbc.NoOffsetStorage
import org.cafienne.service.api.cases.{CaseFile, CaseInstance}
import org.cafienne.service.api.projection.cases.CaseProjectionsWriter
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class CaseFileWriterTest
    extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with WordSpecLike
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
    within(10 seconds) {
      tp.send(storeEventsActor, evt)
      tp.expectMsg(evt)
    }
  }

  val persistence = new TestPersistence()

  val cpw = new CaseProjectionsWriter(persistence, NoOffsetStorage)
  cpw.start()

  val caseInstanceId = "9fc49257_7d33_41cb_b28a_75e665ee3b2c"
  val user = TestIdentityFactory.createTenantUser("test")
  val caseDefinition = TestScript.getCaseDefinition("helloworld.xml")

  val eventFactory = new EventFactory(caseInstanceId, caseDefinition, user)

  val ivm = Instant.now()
  val caseDefinitionApplied = eventFactory.createCaseDefinitionApplied()
  val path = "Greeting"
//  val jsonValue = new ValueMap("Greeting", new ValueMap("Message", "hi there", "From", "admin"))
  val jsonValue = new ValueMap("Message", "hi there", "From", "admin")
  val caseFileEvent = eventFactory.createCaseFileEvent(path, jsonValue, CaseFileItemTransition.Create)
  val caseModifiedEvent = eventFactory.createCaseModified(ivm)

//  def getJSON(value: String): ValueMap =
//    if (value == "" || value == null) new ValueMap
//    else JSONReader.parse(value)

  "CaseProjectionsWriter" must {
    "add and update a case file" in {

      sendEvent(caseDefinitionApplied)
      sendEvent(caseFileEvent)
      sendEvent(caseModifiedEvent)

      val expectedCaseFileContent = """{
                             |  "Greeting" : {
                             |    "Message" : "hi there",
                             |    "From" : "admin"
                             |  }
                             |}""".stripMargin
      Thread.sleep(2000)

      eventually {
        persistence.records.length shouldBe 5
        persistence.records.head shouldBe a[CaseInstance]
        persistence.records(1) match {
          case cs: CaseFile =>
            cs.data should be(expectedCaseFileContent)
          case other => assert(false, "CaseFile object expected, found " + other.getClass.getName)
        }
      }
    }
  }
}
