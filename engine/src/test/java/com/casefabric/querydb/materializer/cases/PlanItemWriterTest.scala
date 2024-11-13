package com.casefabric.querydb.materializer.cases

import com.casefabric.cmmn.actorapi.event.plan.PlanItemCreated
import com.casefabric.cmmn.instance.PlanItemType
import com.casefabric.querydb.materializer.TestQueryDB

import java.time.Instant

class PlanItemWriterTest extends CaseEventSinkTest {

  val planItemCreated: PlanItemCreated = eventFactory.createPlanItemCreated("1", PlanItemType.CasePlan, "HelloWorld", "")

  "CaseEventSink" must {
    "add and update plan items" in {

      sendEvent(eventFactory.createCaseDefinitionApplied())
      sendEvent(planItemCreated)
      sendEvent(eventFactory.createCaseModified(Instant.now))

      import com.casefabric.querydb.record.{CaseRecord, PlanItemRecord}

      eventually {
        assert(TestQueryDB.hasTransaction(caseInstanceId))
        val transaction = TestQueryDB.getTransaction(caseInstanceId)
        transaction.records.length shouldBe 7
        assert(transaction.records.exists(x => x.isInstanceOf[CaseRecord]))
        assert(transaction.records.exists(x => x.isInstanceOf[PlanItemRecord]))
      }
    }
  }
}
