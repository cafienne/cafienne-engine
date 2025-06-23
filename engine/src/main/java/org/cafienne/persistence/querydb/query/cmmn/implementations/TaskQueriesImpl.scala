package org.cafienne.persistence.querydb.query.cmmn.implementations

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.cmmn.filter.TaskFilter
import org.cafienne.persistence.querydb.query.cmmn.implementations.basequeries.TaskAccessHelper
import org.cafienne.persistence.querydb.query.cmmn.{TaskCount, TaskQueries}
import org.cafienne.persistence.querydb.query.exception.TaskSearchFailure
import org.cafienne.persistence.querydb.record.TaskRecord
import org.cafienne.persistence.querydb.schema.QueryDB

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class TaskQueriesImpl(queryDB: QueryDB) extends BaseQueryImpl(queryDB) with TaskAccessHelper with TaskQueries {

  import dbConfig.profile.api._

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

  override def getAllTasks(user: UserIdentity, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {
    // Note: the query to get all tasks for this specific user needs to include membership authorization.
    //  The generic membershipQuery() method that is applied to all queries
    //  proves to be not efficient enough (in terms database execution and query plan and so).
    //  The optimization is not required when we retrieve tasks for a specific assignee.
    //  Therefore, we distinguish first whether a specific assignment on tasks is requested.
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
        // Apply the filters: _1 is the case instance id, _2 is the case role, _3 indicates caseOwnership (one has access either through role or through case ownership)
        consentGroupCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(access => task.role === access._2 || access._3 === true).exists
          || tenantRoleCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(access => task.role === access._2 || access._3 === true).exists
          || userCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(access => task.role === access._2 || access._3 === true).exists)

    // Potentially extend the taskFilter query with a join on the case name and also with the query on the business identifiers
    val query = {
      // First, extend the query to filter on case name as well
      val extendedTaskFilterQuery = withCaseNameFilter(taskFilterQuery, filter.caseName)
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
      baseQuery <- withCaseNameFilter(taskFilterQuery, filter.caseName)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, filter.identifiers)

    } yield baseQuery

    db.run(query.distinct.only(area).order(sort).result)
  }

  private def withCaseNameFilter(taskFilterQuery: Query[TaskTable, TaskRecord, Seq], optionalCaseName: Option[String]): Query[TaskTable, TaskRecord, Seq] = {
    optionalCaseName.fold(taskFilterQuery)(caseName => {
      taskFilterQuery.join(TableQuery[CaseInstanceTable].filter(_.caseName === caseName)).on(_.caseInstanceId === _.id).map(_._1)
    })
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

    // Select all unassigned tasks (optionally from the specified tenant)
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

    implicit val ec: ExecutionContext = db.ioExecutionContext
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
}
