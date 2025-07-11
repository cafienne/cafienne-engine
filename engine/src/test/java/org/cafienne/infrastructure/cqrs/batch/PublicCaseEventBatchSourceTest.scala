package org.cafienne.infrastructure.cqrs.batch

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.event.{Logging, LoggingAdapter}
import org.apache.pekko.persistence.query.{EventEnvelope, Offset}
import org.apache.pekko.stream.RestartSettings
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.apache.pekko.testkit.TestKit
import org.cafienne.engine.cmmn.actorapi.command.StartCase
import org.cafienne.engine.cmmn.actorapi.command.plan.MakePlanItemTransition
import org.cafienne.engine.cmmn.definition.CaseDefinition
import org.cafienne.engine.cmmn.instance.Transition
import org.cafienne.engine.cmmn.test.TestScript
import org.cafienne.engine.cmmn.test.TestScript.{loadCaseDefinition, testUser}
import org.cafienne.infrastructure.config.TestConfig
import org.cafienne.infrastructure.cqrs.batch.public_events.PublicCaseEventBatch
import org.cafienne.json.ValueMap
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
  val source = new TestPublicEventBatchSource(caseSystem)


  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(5, org.scalatest.time.Seconds)),
    interval = scaled(Span(5, org.scalatest.time.Seconds)))

  val caseInstanceId: String = new Guid().toString
  val caseDefinition: CaseDefinition = loadCaseDefinition("testdefinition/public_event_test.xml")
  val startCaseCommand: StartCase = TestScript.createCaseCommand(testUser, caseInstanceId, caseDefinition, new ValueMap("Root", "Test"))
  val triggerUserEvent = new MakePlanItemTransition(testUser, caseInstanceId, "UserEvent", Transition.Occur)
  val completeLaterTask = new MakePlanItemTransition(testUser, caseInstanceId, "LaterTask", Transition.Complete)

  "PublicCaseEventBatchSource" must {
    "publish events" in {

      // We have n number of commands ...
      val commands = Seq(startCaseCommand, triggerUserEvent, completeLaterTask)
//      val commands = Seq(startCaseCommand)
      // ... and we expect also n number of batches to become available for our assertions
      val streamReader: Future[Seq[PublicCaseEventBatch]] = source.publicEvents.take(commands.size).runWith(Sink.seq)


      println(s"Sending ${commands.size} commands: [${commands.map(_.getClass.getSimpleName).mkString(", ")}]")
      whenReady(caseSystem.runCommands(commands))(responses => {
        println(s"Received ${responses.size} responses: [${responses.map(_.getClass.getSimpleName).mkString(", ")}]")
        whenReady(streamReader) { batches =>
          println("All public event batches have been received; checking each of them")

          // Iterate batches and show the ful json of the event
          var number = 0
          batches.foreach(batch => {
            number += 1
            println(s"Batch[$number] has ${batch.publicEvents.size} events:\n- ${batch.publicEvents.map(_.toValue).mkString("\n- ")}\n")
          })

          // Iterate batches again and now just show the summary of the event (type + path)
          number = 0
          batches.foreach(batch => {
            number+=1
            println(s"Batch[$number] has ${batch.publicEvents.size} events:\n- ${batch.publicEvents.map(_.content.toString).mkString("\n- ")}\n")
          })

//          val eventBatch = batches.head
//          println(s"- Validating batch 1: expecting 2 public events - encountered ${eventBatch.publicEvents.size} events (types: ${eventBatch.publicEvents.map(_.content.getClass.getSimpleName).toSet.mkString(", ")})")
//          assert(eventBatch.publicEvents.size == 2)
//
//          val batch2 = batches(1)
//          println(s"- Validating batch 2: expecting 2 public events - encountered ${batch2.publicEvents.size} events (types: ${batch2.publicEvents.map(_.content.getClass.getSimpleName).toSet.mkString(", ")})")
//          assert(batch2.publicEvents.size == 2)

          println("Test completed successfully")
        }
      })
    }
  }

  class TestPublicEventBatchSource(val caseSystem: TestCaseSystem) extends PublicCaseEventBatchSource {
    override def getOffset: Future[Offset] = Future.successful(Offset.noOffset)
    override val system: ActorSystem = caseSystem.actorSystem
    override val restartSettings: RestartSettings = caseSystem.caseSystem.config.persistence.queryDB.restartSettings
    override val readJournal: String = caseSystem.caseSystem.config.persistence.readJournal

    override def query(offset: Offset): Source[EventEnvelope, NotUsed] = {
      journal().eventsByPersistenceId(caseInstanceId, 0, Long.MaxValue)
    }

    override def createBatch(persistenceId: String): PublicCaseEventBatch = {
      println(s"Creating batch for case $persistenceId")
      super.createBatch(persistenceId)
    }
  }
}
