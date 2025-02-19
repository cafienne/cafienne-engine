/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.persistence.querydb.schema.versions

import org.cafienne.persistence.infrastructure.jdbc.SlickTableExtensions
import org.cafienne.persistence.infrastructure.jdbc.schema.QueryDBSchemaVersion
import org.cafienne.persistence.querydb.record.PlanItemHistoryRecord
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.migration.api.TableMigration

import java.time.Instant

class QueryDB_1_1_22(val dbConfig: DatabaseConfig[JdbcProfile], val tablePrefix: String)
  extends QueryDBSchemaVersion
    with CafienneTablesV3 {

  val version = "1.1.22"
  val migrations = dropPlanItemHistoryTable

  import dbConfig.profile.api._

  def dropPlanItemHistoryTable = TableMigration(TableQuery[PlanItemHistoryTable]).drop

}

trait CafienneTablesV3 extends SlickTableExtensions {

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
