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

package org.cafienne.persistence.querydb.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.collection.mutable.{Map, Set}
import scala.concurrent.{ExecutionContext, Future}


trait PlatformQueries {
  def hasExistingUserIds(newUserIds: Seq[String], tenants: Option[Seq[String]]): Future[Seq[String]] = ???

  def whereUsedInTenants(userIds: Seq[String], tenants: Option[Seq[String]]): Future[Map[String, Set[String]]] = ???

  def whereUsedInCases(userIds: Seq[String], tenants: Option[Seq[String]]): Future[Map[(String, String), Set[String]]] = ???
}

class PlatformQueriesImpl(val queryDB: QueryDB) extends QueryDBReader with PlatformQueries with LazyLogging {

  val dbConfig = queryDB.dbConfig

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext

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
      // Note: because of the "drop" of PlanItemHistoryTable, this query no longer gives a reliable result.
      // To make it reliable again, it is required check the case events instead.
      planitems <- TableQuery[PlanItemTable].filter(_.modifiedBy.inSet(userIds)).inTenants(tenants)
    } yield (planitems.caseInstanceId, planitems.tenant, planitems.modifiedBy)

    val taskQuery = for {
      tasks <- TableQuery[TaskTable].filter(record => record.createdBy.inSet(userIds) || record.modifiedBy.inSet(userIds) || record.assignee.inSet(userIds) || record.owner.inSet(userIds)).inTenants(tenants)
    } yield (tasks.caseInstanceId, tasks.tenant, tasks.createdBy, tasks.modifiedBy, tasks.assignee, tasks.owner)

    val teamQuery = for {
      teams <- TableQuery[CaseInstanceTeamUserTable].filter(record => record.userId.inSet(userIds)) //.inTenants(tenants)
      tenants <- TableQuery[CaseInstanceTable].filter(_.id === teams.caseInstanceId).map(_.tenant)
    } yield (teams.caseInstanceId, tenants, teams.userId)

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
