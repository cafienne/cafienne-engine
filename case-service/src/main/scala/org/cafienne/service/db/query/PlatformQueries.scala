package org.cafienne.service.db.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.service.db.schema.table.{CaseTables, TaskTables, TenantTables}

import scala.collection.mutable.{Map, Set}
import scala.concurrent.Future


trait PlatformQueries {
  def hasExistingUserIds(newUserIds: Seq[String], tenants: Option[Seq[String]]): Future[Seq[String]] = ???

  def whereUsedInTenants(userIds: Seq[String], tenants: Option[Seq[String]]): Future[Map[String, Set[String]]] = ???

  def whereUsedInCases(userIds: Seq[String], tenants: Option[Seq[String]]): Future[Map[(String, String), Set[String]]] = ???
}


class PlatformQueriesImpl extends PlatformQueries with LazyLogging
  with TenantTables with CaseTables with TaskTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext

  override def hasExistingUserIds(newUserIds: Seq[String], tenants: Option[Seq[String]]): Future[Seq[String]] = {
    val query = for {
      existingUser <- TableQuery[UserRoleTable].filter(_.role_name === "").filter(_.userId.inSet(newUserIds)).distinctOn(_.userId).inTenants(tenants)
    } yield existingUser.userId // Only select userId and not all fields
    db.run(query.result)
  }

  override def whereUsedInTenants(userIds: Seq[String], tenants: Option[Seq[String]]): Future[Map[String, Set[String]]] = {
    val query = TableQuery[UserRoleTable].filter(_.role_name === "").filter(_.userId.inSet(userIds)).inTenants(tenants)
    db.run(query.result).map(records => {
      val usersPerTenant = scala.collection.mutable.Map[String, scala.collection.mutable.Set[String]]()
      records.map(record => usersPerTenant.getOrElseUpdate(record.tenant, scala.collection.mutable.Set[String]()).add(record.userId))
      usersPerTenant
    })
  }

  override def whereUsedInCases(userIds: Seq[String], tenants: Option[Seq[String]]): Future[Map[(String, String), Set[String]]] = {
    val usersPerCasePerTenant = Map[(String, String), Set[String]]()

    // Method to register a case id, but only if the user is in the list of incoming userIds (this skips users that have also been active in a matching case of task, but then on a different field (modifiedBy vs createdBy)
    // This gives faster queries than doing multiple queries on each column in a for-loop (although that is preciser in it's results)
    def register(id: String, tenant: String, users: Seq[String]) = users.filter(userIds.contains(_)).map(usersPerCasePerTenant.getOrElseUpdate((id, tenant), Set[String]()).add(_))

    val caseQuery = for {
      cases <- TableQuery[CaseInstanceTable].filter(record => record.modifiedBy.inSet(userIds) || record.createdBy.inSet(userIds)).inTenants(tenants)
    } yield (cases.id, cases.tenant, cases.createdBy, cases.modifiedBy)

    val planQuery = for {
      planitems <- TableQuery[PlanItemHistoryTable].filter(_.modifiedBy.inSet(userIds)).inTenants(tenants)
    } yield (planitems.caseInstanceId, planitems.tenant, planitems.modifiedBy)

    val taskQuery = for {
      tasks <- TableQuery[TaskTable].filter(record => record.createdBy.inSet(userIds) || record.modifiedBy.inSet(userIds) || record.assignee.inSet(userIds) || record.owner.inSet(userIds)).inTenants(tenants)
    } yield (tasks.caseInstanceId, tasks.tenant, tasks.createdBy, tasks.modifiedBy, tasks.assignee, tasks.owner)

    val teamQuery = for {
      teams <- TableQuery[CaseInstanceTeamMemberTable].filter(record => record.isTenantUser && record.memberId.inSet(userIds)).inTenants(tenants)
    } yield (teams.caseInstanceId, teams.tenant, teams.memberId)

    val result = for {
      // Register case.id+tenant, case.createdBy and case.modifiedBy
      cases <- db.run(caseQuery.result).map(_.map(record => register(record._1, record._2, Seq(record._3, record._4))))
      // Register planitemhistory.caseInstanceId+tenant and planitemhistory.modifiedBy
      planitems <- db.run(planQuery.result).map(_.map(record => register(record._1, record._2, Seq(record._3))))
      // Register task.caseInstanceId+tenant and task.createdBy, task.modifiedBy, task.assignee, task.owner
      tasks <- db.run(taskQuery.result).map(_.map(record => register(record._1, record._2, Seq(record._3, record._4, record._5, record._6))))
      // Register team.caseInstanceId+tenant and team.memberId
      teams <- db.run(teamQuery.result).map(_.map(record => register(record._1, record._2, Seq(record._3))))
    } yield (cases, planitems, tasks, teams)

    // Return the result map once the queries have completed
    result.map(_ => usersPerCasePerTenant)
  }
}
