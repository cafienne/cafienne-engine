package org.cafienne.persistence.querydb.query.cmmn.implementations

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.cmmn.CaseListQueries
import org.cafienne.persistence.querydb.query.cmmn.filter.CaseFilter
import org.cafienne.persistence.querydb.record.CaseRecord
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.Future


class CaseListQueriesImpl(queryDB: QueryDB)
  extends BaseQueryImpl(queryDB)
    with CaseListQueries {

  import dbConfig.profile.api._


  //  override def getCasesStats(user: UserIdentity, tenant: Option[String], from: Int, numOfResults: Int, caseName: Option[String] = None, status: Option[String] = None): Future[Seq[CaseList]] = {
  //    // TODO
  //    // Query must be converted to:
  //    //   select count(*), case_name, state, failures from case_instance group by case_name, state, failures [having state == and case_name == ]
  //    val definitionFilter = caseName.getOrElse("")
  //    val statusFilter = status.getOrElse("")
  //
  //    val tenantSet = tenant match {
  //      case Some(tenant) => Seq(tenant)
  //      case None => user.tenants
  //    }
  //    // NOTE: this uses a direct query. Fields must be escaped with quotes for the in-memory Hsql Database usage.
  //    val action = {
  //      status match {
  //        //        case Some(s) => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = '#$s' """.as[(Long, String, String, String, Int)]
  //        case Some(s) => s.toLowerCase() match {
  //          case "active" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Active'""".as[(Long, String, String, String, Int)]
  //          case "completed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Completed'""".as[(Long, String, String, String, Int)]
  //          case "terminated" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Terminated'""".as[(Long, String, String, String, Int)]
  //          case "suspended" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Suspended'""".as[(Long, String, String, String, Int)]
  //          case "failed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Failed'""".as[(Long, String, String, String, Int)]
  //          case "closed" => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" having "state" = 'Closed'""".as[(Long, String, String, String, Int)]
  //          case other => throw new SearchFailure(s"Status $other is invalid")
  //        }
  //        case None => sql"""select count("case_name") as count, "tenant", "case_name", "state", "failures" from  "case_instance" group by "tenant", "case_name", "state", "failures" """.as[(Long, String, String, String, Int)]
  //      }
  //
  //    }
  //    db.run(action).map { value =>
  //      val r = collection.mutable.Map.empty[String, CaseList]
  //      value.filter(caseInstance => tenantSet.contains(caseInstance._2)).foreach { caseInstance =>
  //        val count = caseInstance._1
  //        val caseName = caseInstance._3
  //        if (definitionFilter == "" || definitionFilter == caseName) {
  //          // Only add stats for a certain filter if it is the same
  //          val state = caseInstance._4
  //          val failures = caseInstance._5
  //
  //          if (statusFilter == "" || statusFilter == state) {
  //            val caseList: CaseList = r.getOrElse(caseName, CaseList(caseName = caseName))
  //            val newCaseList: CaseList = {
  //              var list: CaseList = state match {
  //                case "Active" => caseList.copy(numActive = caseList.numActive + count)
  //                case "Completed" => caseList.copy(numCompleted = caseList.numCompleted + count)
  //                case "Terminated" => caseList.copy(numTerminated = caseList.numTerminated + count)
  //                case "Suspended" => caseList.copy(numSuspended = caseList.numSuspended + count)
  //                case "Failed" => caseList.copy(numFailed = caseList.numFailed + count)
  //                case "Closed" => caseList.copy(numClosed = caseList.numClosed + count)
  //                case _ => caseList
  //              }
  //              if (failures > 0) {
  //                list = list.copy(numWithFailures = caseList.numWithFailures + count)
  //              }
  //              list
  //            }
  //            r.put(caseName, newCaseList)
  //          }
  //        }
  //      }
  //      r.values.toSeq
  //    }
  //  }

  override def getCases(user: UserIdentity, filter: CaseFilter, area: Area, sort: Sort): Future[Seq[CaseRecord]] = {
    val query = for {
      baseQuery <- statusFilter(filter.status)
        .filterOpt(filter.tenant)((t, value) => t.tenant === value)
        .filterOpt(filter.rootCaseId)((t, value) => t.rootCaseId === value)
        .filterOpt(filter.caseName)((t, value) => t.caseName.toLowerCase like s"%${value.toLowerCase}%")

      // Validate team membership
      _ <- membershipQuery(user, baseQuery.id, filter.identifiers)
    } yield baseQuery
    db.run(query.distinct.only(area).order(sort).result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      records
    })
  }

  def statusFilter(status: Option[String]): Query[CaseInstanceTable, CaseRecord, Seq] = {
    // Depending on the value of the "status" filter, we have 3 different filters.
    // Reason is that admin-ui uses status=Failed for both Failed and "cases with Failures"
    //  Better approach is to simply add a failure count to the case instance and align the UI for that.

    status match {
      case None => caseInstanceQuery
      case Some(state) => state match {
        case "Failed" => caseInstanceQuery.filter(_.failures > 0)
        case other => caseInstanceQuery.filter(_.state === other)
      }
    }
  }
}