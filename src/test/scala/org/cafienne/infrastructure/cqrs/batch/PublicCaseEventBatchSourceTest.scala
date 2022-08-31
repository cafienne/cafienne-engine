package org.cafienne.infrastructure.cqrs.batch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.query.Offset
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import org.cafienne.cmmn.actorapi.command.StartCase
import org.cafienne.cmmn.actorapi.command.plan.MakePlanItemTransition
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.Transition
import org.cafienne.cmmn.test.TestScript
import org.cafienne.cmmn.test.TestScript.{loadCaseDefinition, testUser}
import org.cafienne.infrastructure.config.TestConfig
import org.cafienne.infrastructure.cqrs.batch.public_events.PublicCaseEventBatch
import org.cafienne.util.Guid
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.{ExecutionContextExecutor, Future}

class PublicCaseEventBatchSourceTest
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
    interval = scaled(Span(100, org.scalatest.time.Millis)))

  val caseInstanceId: String = new Guid().toString
  val caseDefinition: CaseDefinition = loadCaseDefinition("testdefinition/helloworld.xml")
  val startCaseCommand: StartCase = TestScript.createCaseCommand(testUser, caseInstanceId, caseDefinition)
  val makeTransition = new MakePlanItemTransition(testUser, caseInstanceId, "Receive Greeting and Send response", Transition.Complete)

  "PublicCaseEventBatchSource" must {
    "publish events" in {

      // We have n number of commands ...
      val commands = Seq(startCaseCommand, makeTransition)
      // ... and we expect also n number of batches to become available for our assertions
      val streamReader: Future[Seq[PublicCaseEventBatch]] = source.publicEvents.take(commands.size).runWith(Sink.seq)


      println(s"Sending ${commands.size} commands: [${commands.map(_.getClass.getSimpleName).mkString(", ")}]")
      whenReady(caseSystem.runCommands(commands))(responses => {
        println(s"Received ${responses.size} responses: [${responses.map(_.getClass.getSimpleName).mkString(", ")}]")
        whenReady(streamReader) { batches =>
          println("All public event batches have been received; checking each of them")
          val eventBatch = batches.head
          println(s"- Validating batch 1: expecting 2 public events - encountered ${eventBatch.publicEvents.size} events (types: ${eventBatch.publicEvents.map(_.content.getClass.getSimpleName).toSet.mkString(", ")})")
          assert(eventBatch.publicEvents.size == 2)

          val batch2 = batches(1)
          println(s"- Validating batch 2: expecting 2 public events - encountered ${batch2.publicEvents.size} events (types: ${batch2.publicEvents.map(_.content.getClass.getSimpleName).toSet.mkString(", ")})")
          assert(batch2.publicEvents.size == 2)

          println("Test completed successfully")
        }
      })
    }
  }
}

class TestPublicEventBatchSource(val system: ActorSystem) extends PublicCaseEventBatchSource {
  override def getOffset: Future[Offset] = Future.successful(Offset.noOffset)
}