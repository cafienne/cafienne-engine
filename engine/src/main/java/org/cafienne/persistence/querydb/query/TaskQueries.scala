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

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.json.{CafienneJson, LongValue, Value, ValueMap}
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.exception.{CaseSearchFailure, TaskSearchFailure}
import org.cafienne.persistence.querydb.query.filter.TaskFilter
import org.cafienne.persistence.querydb.record.TaskRecord
import org.cafienne.persistence.querydb.schema.QueryDB

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.Future

case class TaskCount(claimed: Long, unclaimed: Long) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap("claimed", new LongValue(claimed), "unclaimed", new LongValue(unclaimed))
}

trait TaskQueries {

  def getCaseMembership(taskId: String, user: UserIdentity): Future[CaseMembership] = ???

  def getTask(taskId: String, user: UserIdentity): Future[TaskRecord] = ???

  def getTasksWithCaseName(caseName: String, tenant: Option[String], user: UserIdentity): Future[Seq[TaskRecord]] = ???

  def getCaseTasks(caseInstanceId: String, user: UserIdentity): Future[Seq[TaskRecord]] = ???

  def getAllTasks(user: UserIdentity, filter: TaskFilter = TaskFilter(), area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[TaskRecord]] = ???

  def getCountForUser(user: UserIdentity, tenant: Option[String]): Future[TaskCount] = ???
}

class TaskQueriesImpl(queryDB: QueryDB)
  extends BaseQueryImpl(queryDB)
    with TaskQueries {

  import dbConfig.profile.api._

  override def getCaseMembership(taskId: String, user: UserIdentity): Future[CaseMembership] = {
    val result = for {
      caseId <- {
        db.run(TableQuery[TaskTable].filter(_.id === taskId).map(_.caseInstanceId).result).map(records =>
          if (records.isEmpty) throw TaskSearchFailure(taskId)
          else records.head
        )
      }
      membership <- getCaseMembership(caseId, user, TaskSearchFailure, taskId)
    } yield (caseId, membership)
    result.map(result => {
//      println(s"Found membership for user ${user.id} on task $taskId in case ${result._1}")
      result._2
    })
  }

  override def getTask(taskId: String, user: UserIdentity): Future[TaskRecord] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.id === taskId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(task) => task
      case None => throw TaskSearchFailure(taskId)
    }
  }

  override def getTasksWithCaseName(caseName: String, tenant: Option[String], user: UserIdentity): Future[Seq[TaskRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable]
        .join(TableQuery[CaseInstanceTable]).on(_.caseInstanceId === _.id)
        .filter(_._2.caseName === caseName)
        .filterOpt(tenant)(_._1.tenant === _)
      // Access control query
      _ <- membershipQuery(user, baseQuery._1.caseInstanceId)
    } yield baseQuery._1

    db.run(query.distinct.result)
  }

  override def getCaseTasks(caseInstanceId: String, user: UserIdentity): Future[Seq[TaskRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable]
        .filter(_.caseInstanceId === caseInstanceId)
        // Note: join full may sound heavy, but it is actually only on the case id, and that MUST exist as well.
        //  This helps distinguishing between case-not-found and no-tasks-found-on-existing-case-that-we-have-access-to
        .joinFull(TableQuery[CaseInstanceTable].filter(_.id === caseInstanceId).map(_.id)).on(_.caseInstanceId === _)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.map(_._2).isEmpty) throw CaseSearchFailure(caseInstanceId)
      records.map(_._1).filter(_.nonEmpty).map(_.get)
    })
  }

  override def getAllTasks(user: UserIdentity, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {
    // Note: the query to get all tasks for this specific user needs to include membership authorization.
    //  The generic membershipQuery() method that is applied to all queries
    //  proves to be not efficient enough (in terms database execution and query plan and so).
    //  The optimization is not required when we retrieve tasks for a specific assignee.
    //  Therefore we distinguish first whether or not a specific assignment on tasks is requested.
    filter.assignee match {
      case Some(assignee) => getAssignedTasks(user, assignee, filter, area, sort)
      case None => getUserTasks(user, filter, area, sort)
    }
  }

  def getUserTasks(user: UserIdentity, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {
    // User specific tasks executes the membership authorization in a different manner, namely through more close
    //  querying on membership in relation to the task table, instead of going via the case instance tables.

    val taskFilterQuery = TableQuery[TaskTable]
      .filterOpt(filter.tenant)(_.tenant === _)
      .filterOpt(filter.taskName)(_.taskName === _)
      // There can be multiple task states passed, as a semi-colon separated string. If any, extend the query for those.
      .filterOpt(filter.taskState)((row, state) => row.taskState inSet state.split(";"))
      .filterOpt(filter.owner)(_.owner === _)
      .filterOpt(filter.dueOn)(_.dueDate >= getStartDate(_, filter.timeZone))
      .filterOpt(filter.dueOn)(_.dueDate <= getEndDate(_, filter.timeZone))
      .filterOpt(filter.dueBefore)(_.dueDate < getStartDate(_, filter.timeZone))
      .filterOpt(filter.dueAfter)(_.dueDate > getEndDate(_, filter.timeZone))
      .filter(task =>
        // Apply the filters: _1 is the case instance id, _2 is the case role
        consentGroupCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
          || tenantRoleCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
          || userCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists)

    // Potentially extend the taskFilter query with a join on the case name and also with the query on the business identifiers
    val query = {
      // First, extend the query to filter on case name as well
      val extendedTaskFilterQuery = filter.caseName.fold(taskFilterQuery)(caseName => taskFilterQuery.join(caseInstanceQuery.filter(_.caseName === caseName)).on(_.caseInstanceId === _.id).map(_._1))
      // Next, extend for business identifiers (if necessary)
      if (filter.identifiers.nonEmpty) {
        for {
          baseQuery <- extendedTaskFilterQuery
          _ <- new BusinessIdentifierFilterParser(filter.identifiers).asQuery(baseQuery.caseInstanceId)
        } yield baseQuery
      } else {
        extendedTaskFilterQuery
      }
    }

    db.run(query.distinct.only(area).order(sort).result)
  }

  def getAssignedTasks(user: UserIdentity, assignee: String, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {
    val taskFilterQuery = TableQuery[TaskTable]
      .filter(_.assignee === assignee)
      .filterOpt(filter.tenant)(_.tenant === _)
      .filterOpt(filter.taskName)(_.taskName === _)
      // There can be multiple task states passed, as a semi-colon separated string. If any, extend the query for those.
      .filterOpt(filter.taskState)((row, state) => row.taskState inSet state.split(";"))
      .filterOpt(filter.owner)(_.owner === _)
      .filterOpt(filter.dueOn)(_.dueDate >= getStartDate(_, filter.timeZone))
      .filterOpt(filter.dueOn)(_.dueDate <= getEndDate(_, filter.timeZone))
      .filterOpt(filter.dueBefore)(_.dueDate < getStartDate(_, filter.timeZone))
      .filterOpt(filter.dueAfter)(_.dueDate > getEndDate(_, filter.timeZone))

    val query = for {
      baseQuery <- {
        // Potentially extend the taskFilter query with a join on the case name
        filter.caseName.fold(taskFilterQuery)(caseName => {
          taskFilterQuery.join(caseInstanceQuery.filter(_.caseName === caseName)).on(_.caseInstanceId === _.id).map(_._1)
        })
      }

      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, filter.identifiers)

    } yield baseQuery

    db.run(query.distinct.only(area).order(sort).result)
  }

  private def getStartDate(date: String, timeZone: Option[String]): Option[Instant] = {
    Option(LocalDateTime.parse(date + "T00:00:00.000000000").toInstant(getTimeZoneOffset(timeZone)))
  }

  private def getTimeZoneOffset(timeZone: Option[String]): ZoneOffset = {
    timeZone match {
      case Some(offsetId) => ZoneOffset.of(offsetId)
      case None => ZoneOffset.UTC // TODO: Acceptable default, but is it backwards compatible?
    }
  }

  private def getEndDate(date: String, timeZone: Option[String]): Option[Instant] = {
    Option(LocalDateTime.parse(date + "T23:59:59.999999999").toInstant(getTimeZoneOffset(timeZone)))
  }

  override def getCountForUser(user: UserIdentity, tenant: Option[String]): Future[TaskCount] = {
    val claimedTasksQuery = TableQuery[TaskTable].filter(_.assignee === user.id)
      .filterNot(_.taskState === "Completed")
      .filterNot(_.taskState === "Terminated")
      .filterOpt(tenant)(_.tenant === _)

    val unclaimedTasksQuery = for {
      // Select all unassigned tasks (optionally from the specified tenant)
      unclaimedTasks <- TableQuery[TaskTable].filter(_.assignee === "").filterOpt(tenant)(_.tenant === _)
        // And only active tasks, not completed or terminated
        .filterNot(_.taskState === "Completed").filterNot(_.taskState === "Terminated")
        .filter(task =>
          // Apply the filters: _1 is the case instance id, _2 is the case role
          consentGroupCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || tenantRoleCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || userCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists)
    } yield unclaimedTasks

    val count = for {
      claimedTasks <- db.run(claimedTasksQuery.distinct.length.result)
      unclaimedTasks <- db.run(unclaimedTasksQuery.distinct.length.result)
    } yield (claimedTasks, unclaimedTasks)

    count.map(c => {
      val tc = TaskCount(c._1, c._2)
      //      println("Returning Count: : " + tc)
      tc
    })
  }

  // First define 3 base queries that help find the cases that the user has access to with the specific case roles.
  //  Resulting queries give a list of case instance id / case role pairs.
  private def consentGroupCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamGroupTable]).on((group, membership) => group.role === membership.groupRole && group.group === membership.groupId).map(_._2)
      .map(group => (group.caseInstanceId, group.caseRole))
  }

  private def tenantRoleCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    tenantRoleQuery(user)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable])
      .on((tenantRoles, caseMembership) =>
        // Either
        (tenantRoles._1 === caseMembership.tenantRole)
          && tenantRoles._2 === caseMembership.tenant).map(_._2)
      .map(tenantRole => (tenantRole.caseInstanceId, tenantRole.caseRole))
  }

  private def userCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === user.id)
      .map(user => (user.caseInstanceId, user.caseRole))
  }
}
