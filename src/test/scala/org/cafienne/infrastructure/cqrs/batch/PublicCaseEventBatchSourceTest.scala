package org.cafienne.infrastructure.cqrs.batch

import akka.{Done, NotUsed}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.persistence.query.Offset
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.{TestKit, TestProbe}
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned
import org.cafienne.cmmn.definition.CaseDefinition
import org.cafienne.cmmn.instance.{State, Transition}
import org.cafienne.cmmn.test.TestScript
import org.cafienne.identity.TestIdentityFactory
import org.cafienne.infrastructure.config.TestConfig
import org.cafienne.infrastructure.cqrs.batch.public_events.PublicCaseEventBatch
import org.cafienne.querydb.materializer.EventFactory
import org.cafienne.querydb.materializer.cases.CreateEventsInStoreActor
import org.cafienne.util.Guid
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpecLike

import java.time.Instant
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class PublicCaseEventBatchSourceTest
  extends TestKit(ActorSystem("testsystem", TestConfig.config))
    with ScalaFutures
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Eventually {

  implicit val logger: LoggingAdapter = Logging(system, getClass)
  implicit val executor: ExecutionContextExecutor = system.dispatcher
  val testActorSystem: ActorSystem = this.system

  val caseInstanceId: String = new Guid().toString

  lazy val storeEventsActor: ActorRef = system.actorOf(Props(classOf[CreateEventsInStoreActor]), caseInstanceId)
  val tp: TestProbe = TestProbe()

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(5,  org.scalatest.time.Seconds)),
    interval = scaled(Span(100,  org.scalatest.time.Millis)))

  def sendEvent(evt: CaseEvent): CaseEvent = {
    within(10.seconds) {
      tp.send(storeEventsActor, evt)
      tp.expectMsg(evt)
    }
  }

  val user: TenantUser = TestIdentityFactory.createTenantUser("test")
  val caseDefinition: CaseDefinition = TestScript.getCaseDefinition("testdefinition/helloworld.xml")
  val eventFactory = new EventFactory(caseInstanceId, caseDefinition, user)

  println("Creating public event source")
  //new CaseEventSink(new CaseSystem(system).system, TestQueryDB).start()
  val publicEventsBatchSource: Source[PublicCaseEventBatch, NotUsed] = new PublicCaseEventBatchSource {
    /**
      * Provide the offset from which we should start sourcing events
      */
    override def getOffset: Future[Offset] = Future.successful(Offset.noOffset)

    /**
      * Tag to scan events for
      */
    override val tag: String = CaseEvent.TAG

    override def system: ActorSystem = testActorSystem
  }.publicEvents

  //  ..onComplete {
  //    case Success(_) => system.log.info("Done processing all public events")//
  //    case Failure(ex) => fail("Error completing", ex)
  //  }
  val milestoneCreated: PlanItemTransitioned = eventFactory.createPlanItemTransitioned("1", "Milestone", State.Available, State.Completed, Transition.Occur)

  "PublicCaseEventBatchSource" must {
    "publish events" in {

      println("Sending events")
      sendEvent(eventFactory.createCaseDefinitionApplied())
      sendEvent(milestoneCreated)
      sendEvent(eventFactory.createCaseModified(Instant.now()))

      def streamReader: Future[PublicCaseEventBatch] = publicEventsBatchSource.runWith(Sink.head)

      whenReady(streamReader) { eventBatch =>
        println("Checking stream runner completion iwth result" + eventBatch.publicEvents.size)
        assert(eventBatch.publicEvents.size == 2)
      }

//        whenReady(streamRunner) { runResult =>
//        logger.debug("runResult: {}", runResult)
//        //assert(replayResult.offset.contains(Sequence(1L)))
//      }

    }
  }
}
