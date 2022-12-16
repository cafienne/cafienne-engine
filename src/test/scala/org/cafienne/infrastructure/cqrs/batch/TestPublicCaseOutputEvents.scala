package org.cafienne.infrastructure.cqrs.batch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.query.{EventEnvelope, Offset}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.test.TestScript
import org.cafienne.cmmn.test.TestScript.{loadCaseDefinition, testUser}
import org.cafienne.humantask.actorapi.command.CompleteHumanTask
import org.cafienne.infrastructure.config.TestConfig
import org.cafienne.infrastructure.cqrs.batch.public_events.{CaseCompleted, HumanTaskStarted, PublicCaseEventBatch}
import org.cafienne.json.ValueMap
import org.cafienne.util.Guid
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContextExecutor, Future}

class TestPublicCaseOutputEvents
  extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with ScalaFutures
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  implicit val logger: LoggingAdapter = Logging(system, getClass)
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  val caseSystem: TestCaseSystem = new TestCaseSystem(system)
  val source = new TestPublicEventBatchSource(system)


  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(5, org.scalatest.time.Seconds)),
    interval = scaled(Span(5, org.scalatest.time.Seconds)))

  val caseInstanceId: String = new Guid().toString
  val caseDefinition: CaseDefinition = loadCaseDefinition("testdefinition/case_with_output.xml")
  val startCaseCommand: StartCase = TestScript.createCaseCommand(testUser, caseInstanceId, caseDefinition, new ValueMap("CaseInput", new ValueMap("Input", "Any input goes")))

  "TestPublicCaseOutputEvents" must {
    "publish and receive events" in {

      // We have n number of commands ...
      val commands = Seq(startCaseCommand)
      // ... and we expect also n number of batches to become available for our assertions
      val startCaseReader: Future[Seq[PublicCaseEventBatch]] = source.publicEvents.take(1).runWith(Sink.seq)


      println(s"Sending ${commands.size} commands: [${commands.map(_.getClass.getSimpleName).mkString(", ")}]")
      whenReady(caseSystem.sendCommand(startCaseCommand))(response => {
        println(s"Received $response response: [${response.getClass.getSimpleName}]")
        whenReady(startCaseReader) { batch =>
          println("Public event batches have been received; checking each of them")
          val taskReader: Future[Seq[PublicCaseEventBatch]] = source.publicEvents.take(2).runWith(Sink.seq)

          val humanTaskId = batch.head.publicEvents.map(_.content).filter(_.isInstanceOf[HumanTaskStarted]).map(_.asInstanceOf[HumanTaskStarted]).map(_.taskId).head
          val taskOutput = new ValueMap("Output", "Output can be anything")
          val completeLaterTask = new CompleteHumanTask(testUser, caseInstanceId, humanTaskId, new ValueMap("Result", taskOutput))

          whenReady(caseSystem.sendCommand(completeLaterTask))(_ => {
            whenReady(taskReader) { secondBatch => {
              println("Received second batch: " + secondBatch.last)
              val batch = secondBatch.last
              println(s"Batch[2] has ${batch.publicEvents.size} events:\n- ${batch.publicEvents.map(_.content.toValue).mkString("\n- ")}\n")
              val caseCompletedEvents = batch.publicEvents(classOf[CaseCompleted])
              if (caseCompletedEvents.isEmpty || caseCompletedEvents.size > 1) {
                throw new Error("Expecting only 1 case completed event in this batch")
              }
              val caseCompleted = caseCompletedEvents.head
              val expectedCaseOutput = new ValueMap("CaseOutput", taskOutput)
              if (! caseCompleted.output.equals(expectedCaseOutput)) {
                System.err.println(s"Case output mismatch!!!\nFound json ${caseCompleted.output}\n\nExpected json: $expectedCaseOutput")
                throw new Error("Expecting a different case output")
              }
            }

            }
          })

          println("Test completed successfully")
        }
      })
    }
  }

  class TestPublicEventBatchSource(val system: ActorSystem) extends PublicCaseEventBatchSource {
    override def getOffset: Future[Offset] = Future.successful(Offset.noOffset)

    override def query(offset: Offset): Source[EventEnvelope, NotUsed] = {
      journal().eventsByPersistenceId(caseInstanceId, 0, Long.MaxValue)
    }

    override def createBatch(persistenceId: String): PublicCaseEventBatch = {
      println(s"Creating batch for case $persistenceId")
      super.createBatch(persistenceId)
    }
  }
}
