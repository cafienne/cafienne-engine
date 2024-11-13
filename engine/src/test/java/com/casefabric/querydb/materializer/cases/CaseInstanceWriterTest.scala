package com.casefabric.querydb.materializer.cases

import com.casefabric.json.ValueMap
import com.casefabric.querydb.materializer.TestQueryDB

import java.time.Instant

class CaseInstanceWriterTest extends CaseEventSinkTest {

  val emptyCaseFile: String = new ValueMap().toString

  "CaseEventSink" must {
    "add a case instance" in {
      sendEvent(eventFactory.createCaseDefinitionApplied())
      sendEvent(eventFactory.createCaseModified(Instant.now()))

      import com.casefabric.infrastructure.cqrs.offset.OffsetRecord
      import com.casefabric.querydb.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, CaseRoleRecord}

      eventually {
        assert(TestQueryDB.hasTransaction(caseInstanceId))
        val transaction = TestQueryDB.getTransaction(caseInstanceId)
        // A 'simple' CaseDefinitionApplied results always in 6 records, as below, with an empty case file record
        transaction.records.length shouldBe 6 // Events generate below 6 records
        transaction.records.count(_.isInstanceOf[CaseDefinitionRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[CaseRoleRecord]) shouldBe 2
        transaction.records.count(_.isInstanceOf[CaseRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[CaseFileRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[OffsetRecord]) shouldBe 1
        transaction.records.find(_.isInstanceOf[CaseFileRecord]) match {
          case Some(cs: CaseFileRecord) => cs.data shouldBe emptyCaseFile
          case other => assert(false, "Empty CaseFile object expected, found " + other.getClass.getName)
        }
      }
    }
  }
}
