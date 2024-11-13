package com.casefabric.querydb.materializer.cases

import com.casefabric.cmmn.actorapi.event.file.CaseFileItemTransitioned
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition
import com.casefabric.json.ValueMap
import com.casefabric.querydb.materializer.TestQueryDB

import java.time.Instant

class CaseFileWriterTest extends CaseEventSinkTest {

  val path = "Greeting"
  val jsonValue = new ValueMap("Message", "hi there", "From", "admin")
  val caseFileEvent: CaseFileItemTransitioned = eventFactory.createCaseFileEvent(path, jsonValue, CaseFileItemTransition.Create)

  "CaseEventSink" must {
    "add and update a case file" in {

      sendEvent(eventFactory.createCaseDefinitionApplied())
      sendEvent(caseFileEvent)
      sendEvent(eventFactory.createCaseModified(Instant.now()))

      val expectedCaseFileContent = """{
                             |  "Greeting" : {
                             |    "Message" : "hi there",
                             |    "From" : "admin"
                             |  }
                             |}""".stripMargin
      import com.casefabric.infrastructure.cqrs.offset.OffsetRecord
      import com.casefabric.querydb.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, CaseRoleRecord}

      eventually {
        assert(TestQueryDB.hasTransaction(caseInstanceId))
        val transaction = TestQueryDB.getTransaction(caseInstanceId)
        transaction.records.length shouldBe 6 // Events generate below 6 records
        transaction.records.count(_.isInstanceOf[CaseDefinitionRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[CaseRoleRecord]) shouldBe 2
        transaction.records.count(_.isInstanceOf[CaseRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[CaseFileRecord]) shouldBe 1
        transaction.records.count(_.isInstanceOf[OffsetRecord]) shouldBe 1
        transaction.records.find(_.isInstanceOf[CaseFileRecord]) match {
          case Some(cs: CaseFileRecord) => cs.data shouldBe expectedCaseFileContent
          case other => assert(false, "CaseFile object expected, found " + other.getClass.getName)
        }
      }
    }
  }
}
