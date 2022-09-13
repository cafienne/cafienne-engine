package org.cafienne.querydb.schema.versions

import org.cafienne.infrastructure.jdbc.schema.DbSchemaVersion
import org.cafienne.querydb.record.PlanItemHistoryRecord
import org.cafienne.querydb.schema.QueryDBSchema
import slick.migration.api.TableMigration

import java.time.Instant

object QueryDB_1_1_22 extends DbSchemaVersion
  with QueryDBSchema
  with CafienneTablesV3 {

  val version = "1.1.22"
  val migrations = dropPlanItemHistoryTable

  import dbConfig.profile.api._

  def dropPlanItemHistoryTable = TableMigration(TableQuery[PlanItemHistoryTable]).drop

}

trait CafienneTablesV3 extends QueryDBSchema {

  import dbConfig.profile.api._

  final class PlanItemHistoryTable(tag: Tag) extends CafienneTenantTable[PlanItemHistoryRecord](tag, "plan_item_history") {

    lazy val id = idColumn[String]("id", O.PrimaryKey)

    lazy val planItemId = idColumn[String]("plan_item_id")

    lazy val stageId = idColumn[String]("stage_id")

    lazy val name = column[String]("name")

    lazy val index = column[Int]("index")

    lazy val caseInstanceId = idColumn[String]("case_instance_id")

    lazy val currentState = stateColumn[String]("current_state")

    lazy val historyState = stateColumn[String]("history_state")

    lazy val transition = stateColumn[String]("transition")

    lazy val planItemType = stateColumn[String]("plan_item_type")

    lazy val repeating = column[Boolean]("repeating")

    lazy val required = column[Boolean]("required")

    lazy val lastModified = column[Instant]("last_modified")

    lazy val modifiedBy = userColumn[String]("modified_by")

    lazy val eventType = column[String]("eventType")

    lazy val sequenceNr = column[Long]("sequenceNr")

    lazy val taskInput = jsonColumn[String]("task_input")

    lazy val taskOutput = jsonColumn[String]("task_output")

    lazy val mappedInput = jsonColumn[String]("mapped_input")

    lazy val rawOutput = jsonColumn[String]("raw_output")

    lazy val * = (id, planItemId, stageId, name, index, caseInstanceId, tenant, currentState, historyState, transition, planItemType, repeating, required, lastModified, modifiedBy, eventType, sequenceNr, taskInput, taskOutput, mappedInput, rawOutput).mapTo[PlanItemHistoryRecord]

    lazy val idx = index("idx_plan_item_history__plain_item_id", planItemId)
    lazy val indexModifiedBy = oldStyleIndex(modifiedBy)
  }
}
