package org.cafienne.service.akkahttp.writer

import org.cafienne.cmmn.actorapi.event.plan.PlanItemTransitioned
import org.cafienne.cmmn.instance.{State, Transition}

import java.time.Instant

class CaseTaskWriterTest extends CaseEventSinkTest {
  val planItemTransitioned: PlanItemTransitioned = eventFactory.createPlanItemTransitioned("1", "HumanTask", State.Terminated, State.Active, Transition.Terminate)

  "CaseProjectionsWriter" must {
    "add and update tasks" in {

      sendEvent(eventFactory.createCaseDefinitionApplied())
      sendEvent(planItemTransitioned)
      sendEvent(eventFactory.createCaseModified(Instant.now))

      import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
      import org.cafienne.querydb.record._

      eventually {
        assert(TestQueryDB.hasTransaction(caseInstanceId))
        val transaction = TestQueryDB.getTransaction(caseInstanceId)
        println(s"Found ${transaction.records.length} records, of types ${transaction.records.map(_.getClass.getSimpleName).toSet.mkString(",")}")
        transaction.records.length shouldBe 7
        transaction.records.count(_.isInstanceOf[CaseDefinitionRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[CaseRoleRecord]) shouldBe 2
        transaction.records.count(_.isInstanceOf[CaseRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[CaseFileRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[PlanItemHistoryRecord]) shouldBe 1  // so ... then why is this called CaseTaskWriterTest???
        transaction.records.count(_.isInstanceOf[OffsetRecord]) shouldBe 1
      }
    }
  }
}
