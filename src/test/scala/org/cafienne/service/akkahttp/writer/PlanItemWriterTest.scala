package org.cafienne.service.akkahttp.writer

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated

import java.time.Instant

class PlanItemWriterTest extends CaseEventSinkTest {

  val planItemCreated: PlanItemCreated = eventFactory.createPlanItemCreated("1", "CasePlan", "HelloWorld", "")

  "CaseEventSink" must {
    "add and update plan items" in {

      sendEvent(eventFactory.createCaseDefinitionApplied())
      sendEvent(planItemCreated)
      sendEvent(eventFactory.createCaseModified(Instant.now))

      import org.cafienne.querydb.record.{CaseRecord, PlanItemRecord}

      eventually {
        assert(TestQueryDB.hasTransaction(caseInstanceId))
        val transaction = TestQueryDB.getTransaction(caseInstanceId)
        transaction.records.length shouldBe 8
        assert(transaction.records.exists(x => x.isInstanceOf[CaseRecord]))
        assert(transaction.records.exists(x => x.isInstanceOf[PlanItemRecord]))
      }
    }
  }
}
