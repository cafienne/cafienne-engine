package org.cafienne.querydb.query

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.json.{CafienneJson, LongValue, Value, ValueMap}
import org.cafienne.querydb.query.exception.{CaseSearchFailure, TaskSearchFailure}
import org.cafienne.querydb.query.filter.TaskFilter
import org.cafienne.querydb.record.TaskRecord

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

  def getAllTasks(user: UserIdentity, filter: TaskFilter = TaskFilter.Empty, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[TaskRecord]] = ???

  def getCountForUser(user: UserIdentity, tenant: Option[String]): Future[TaskCount] = ???
}


class TaskQueriesImpl extends TaskQueries
  with BaseQueryImpl {

  import dbConfig.profile.api._

  val tasksQuery = TableQuery[TaskTable]

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
      baseQuery <- TableQuery[TaskTable].filter(_.caseInstanceId === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      records
    })
  }

  override def getAllTasks(user: UserIdentity, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {
    // If there is no assignee given, then we need to query tasks that have a role that the user also has.
    //  Otherwise the query will not filter on roles
    val assignmentFilterQuery = filter.assignee match {
      case Some(assignee) => TableQuery[TaskTable].filter(_.assignee === assignee)
      case None =>
        // Select all tasks and filter for:
        //  - consent groups that the user belongs to and that have the task.role equal to the users consent group roles
        //  - tenant roles that the user has and that have a task role equals to the case role associated to that tenant role
        //  - tasks that have a case role associated that the user has directly in the case team
        TableQuery[TaskTable].filter(task =>
          // Apply the filters: _1 is the case instance id, _2 is the case role
          consentGroupCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || tenantRoleCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || userCoupledCaseRoles(user).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists)
    }

    val query = for {
      caseNameFilter <- caseInstanceQuery.filterOpt(filter.caseName)(_.caseName === _)

      baseQuery <- assignmentFilterQuery
        .filter(_.caseInstanceId === caseNameFilter.id)
        .filterOpt(filter.tenant)(_.tenant === _)
        .filterOpt(filter.taskName)(_.taskName === _)
        .filterOpt(filter.taskState)(_.taskState === _)
        .filterOpt(filter.owner)(_.owner === _)
        .filterOpt(filter.dueOn)(_.dueDate >= getStartDate(_, filter.timeZone))
        .filterOpt(filter.dueOn)(_.dueDate <= getEndDate(_, filter.timeZone))
        .filterOpt(filter.dueBefore)(_.dueDate < getStartDate(_, filter.timeZone))
        .filterOpt(filter.dueAfter)(_.dueDate > getEndDate(_, filter.timeZone))

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
      .join(TableQuery[CaseInstanceTeamGroupTable]).on(_.role === _.groupRole).map(_._2)
      .map(group => (group.caseInstanceId, group.caseRole))
  }

  private def tenantRoleCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[UserRoleTable]
      .filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable])
      .on((left, right) => left.role_name === right.tenantRole && left.tenant === right.tenant)
      .map(_._2)
      .map(tenantRole => (tenantRole.caseInstanceId, tenantRole.caseRole))
  }

  private def userCoupledCaseRoles(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === user.id)
      .map(user => (user.caseInstanceId, user.caseRole))
  }
}
