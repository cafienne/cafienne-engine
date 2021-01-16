package org.cafienne.service.api.projection.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.service.api.projection.table.{CaseTables, TaskTables, TenantTables}

import scala.concurrent.Future
import scala.collection.mutable.Map
import scala.collection.mutable.Set


trait PlatformQueries {
  def hasExistingUserIds(newUserIds: Seq[String]): Future[Seq[String]] = ???

  def whereUsedInTenants(userIds: Seq[String]): Future[Map[String, Set[String]]] = ???

  def whereUsedInCases(userIds: Seq[String]): Future[Map[String, Set[String]]] = ???
}


class PlatformQueriesImpl extends PlatformQueries with LazyLogging
  with TenantTables with CaseTables with TaskTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext

  override def hasExistingUserIds(newUserIds: Seq[String]): Future[Seq[String]] = {
    val query = TableQuery[UserRoleTable].filter(_.userId.inSet(newUserIds)).distinctOn(_.userId)
    db.run(query.result).map(records => records.map(record => record.userId))
  }

  override def whereUsedInTenants(userIds: Seq[String]): Future[Map[String, Set[String]]] = {
    val query = TableQuery[UserRoleTable].filter(_.role_name === "").filter(_.userId.inSet(userIds))
    db.run(query.result).map(records => {
      val usersPerTenant = scala.collection.mutable.Map[String, scala.collection.mutable.Set[String]]()
      records.map(record => usersPerTenant.getOrElseUpdate(record.tenant, scala.collection.mutable.Set[String]()).add(record.userId))
      usersPerTenant
    })
  }

  override def whereUsedInCases(userIds: Seq[String]): Future[Map[String, Set[String]]] = {
    val query = for {
      cases <- TableQuery[CaseInstanceTable].filter(record => record.modifiedBy.inSet(userIds) || record.createdBy.inSet(userIds))
      planitems <- TableQuery[PlanItemHistoryTable].filter(_.modifiedBy.inSet(userIds))
      tasks <- TableQuery[TaskTable].filter(record => record.createdBy.inSet(userIds) || record.modifiedBy.inSet(userIds) || record.assignee.inSet(userIds) || record.owner.inSet(userIds))
      // Selective yield below only for relevant columns (and especially not task input/output as they may be heavy and are not relevant)
    } yield (cases.id, cases.createdBy, cases.modifiedBy, planitems.caseInstanceId, planitems.modifiedBy, tasks.caseInstanceId, tasks.createdBy, tasks.modifiedBy, tasks.assignee, tasks.owner)

    db.run(query.result).map(records => {
      val usersPerCase = scala.collection.mutable.Map[String, scala.collection.mutable.Set[String]]()
      // Method to register a case id, but only if the user is in the list of incoming userIds (this skips users that have also been active in a matching case of task, but then on a different field (modifiedBy vs createdBy)
      // This gives faster queries than doing multiple queries on each column in a for-loop (although that is preciser in it's results)
      def register(id: String, users: Seq[String]) = users.filter(userIds.contains(_)).map(usersPerCase.getOrElseUpdate(id, scala.collection.mutable.Set[String]()).add(_))

      records.map(record => {
        // Register case.id, case.createdBy and case.modifiedBy
        register(record._1, Seq(record._2, record._3))
        // Register planitemhistory.caseInstanceId and planitemhistory.modifiedBy
        register(record._4, Seq(record._5))
        // Register task.caseInstanceId and task.createdBy, task.modifiedBy, task.assignee, task.owner
        register(record._6, Seq(record._7, record._8, record._9, record._10))
      })
      usersPerCase
    })
  }
}
