package org.cafienne.querydb.materializer.cases

import org.cafienne.cmmn.actorapi.event.file.CaseFileItemTransitioned
import org.cafienne.cmmn.instance.casefile.CaseFileItemTransition
import org.cafienne.json.ValueMap
import org.cafienne.querydb.materializer.TestQueryDB

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
      import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
      import org.cafienne.querydb.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, CaseRoleRecord}

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
