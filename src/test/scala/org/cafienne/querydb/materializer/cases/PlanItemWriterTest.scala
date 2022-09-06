package org.cafienne.querydb.materializer.cases

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated
import org.cafienne.cmmn.instance.PlanItemType
import org.cafienne.querydb.materializer.TestQueryDB

import java.time.Instant

class PlanItemWriterTest extends CaseEventSinkTest {

  val planItemCreated: PlanItemCreated = eventFactory.createPlanItemCreated("1", PlanItemType.CasePlan, "HelloWorld", "")

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
