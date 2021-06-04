package org.cafienne.timerservice.persistence.jdbc

import org.cafienne.infrastructure.jdbc.CafienneJDBCConfig

import java.time.Instant

/**
  * final case class TimerServiceRecord(timerId: String,
  * caseInstanceId: String,
  * moment: Instant,
  * tenant: String,
  * user: String)
  */
trait TimerServiceTables extends CafienneJDBCConfig {
  import dbConfig.profile.api._

  // Schema for the "task" table:
  final class TimerServiceTable(tag: Tag) extends CafienneTable[TimerServiceRecord](tag, "timer") {

    def timerId = idColumn[String]("timer_id", O.PrimaryKey)

    def caseInstanceId = idColumn[String]("case_instance_id")

    def moment = column[Instant]("moment")

    def tenant = idColumn[String]("tenant")

    def user = column[String]("user", O.Default(""))

    // Various indices for optimizing getAllTasks queries
    def indexCaseInstanceId = index(caseInstanceId)
    def indexTimerId = index(timerId)
    def indexTenant = index(tenant)
    def indexMoment = index(generateIndexName(moment), moment)

    def * = (timerId, caseInstanceId, moment, tenant, user).mapTo[TimerServiceRecord]
  }
}
