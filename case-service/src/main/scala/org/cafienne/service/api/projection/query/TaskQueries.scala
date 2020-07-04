package org.cafienne.service.api.projection.query

import java.time.{Instant, LocalDateTime, ZoneOffset}

import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.cmmn.instance.casefile.{LongValue, Value, ValueMap}
import org.cafienne.infrastructure.json.CafienneJson
import org.cafienne.service.api.projection.record.TaskRecord
import org.cafienne.service.api.projection.{CaseSearchFailure, TaskSearchFailure}

import scala.concurrent.Future

case class TaskCount(claimed: Long, unclaimed: Long) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap("claimed", new LongValue(claimed), "unclaimed", new LongValue(unclaimed))
}

trait TaskQueries {


  // TODO: incorporate the casedefinition info, case parent id and case root id. For all queries. By joining.

  def getTask(taskId: String, user: PlatformUser): Future[TaskRecord] = ???

  def getTasksWithCaseName(caseName: String, tenant: Option[String], user: PlatformUser): Future[Seq[TaskRecord]] = ???

  def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[TaskRecord]] = ???

  def authorizeTaskAccessAndReturnCaseAndTenantId(taskId: String, user: PlatformUser): Future[(String, String)] = ???

  def getAllTasks(user: PlatformUser, filter: TaskFilter = TaskFilter.Empty, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[TaskRecord]] = ???

  def getCountForUser(user: PlatformUser, tenant: Option[String]): Future[TaskCount] = ???
}


class TaskQueriesImpl extends TaskQueries
  with BaseQueryImpl {

  import dbConfig.profile.api._

  val tasksQuery = TableQuery[TaskTable]

  override def getTask(taskId: String, user: PlatformUser): Future[TaskRecord] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.id === taskId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(task) => task
      case None => throw TaskSearchFailure(taskId)
    }
  }

  override def getTasksWithCaseName(caseName: String, tenant: Option[String], user: PlatformUser): Future[Seq[TaskRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable]
        .join(TableQuery[CaseInstanceTable]).on(_.caseInstanceId === _.id)
        .filter(_._2.caseName === caseName)
        .filterOpt(tenant)(_._1.tenant === _)
      // Access control query
      _ <- membershipQuery(user, baseQuery._1.caseInstanceId, baseQuery._1.tenant, None)
    } yield baseQuery._1

    db.run(query.distinct.result)
  }

  override def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[TaskRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.caseInstanceId === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      records
    })
  }

  override def blankIdentifierFilterQuery(caseInstanceId: Rep[String]) = {
    TableQuery[TaskTable].filter(_.caseInstanceId === caseInstanceId)
  }

  override def authorizeTaskAccessAndReturnCaseAndTenantId(taskId: String, user: PlatformUser): Future[(String, String)] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.id === taskId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)

    } yield (baseQuery.caseInstanceId, baseQuery.tenant)

    db.run(query.result.headOption).map {
      case None => throw TaskSearchFailure(taskId)
      case Some(result) => result
    }
  }

  override def getAllTasks(user: PlatformUser, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {

    // If there is no assignee given, then we need to query tasks that have a role that the user also has.
    //  Otherwise the query will not filter on roles
    val assignmentFilterQuery = filter.assignee match {
      case Some(assignee) => TableQuery[TaskTable].filter(_.assignee === assignee)
      case None => for {
        // Select all tasks
        tasks <- TableQuery[TaskTable]
        // In tenants where i am a user
        tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === tasks.tenant)
        // Where my case team roles map to the task role (or where i am a case owner)
        tasksForMyCaseRoles <- TableQuery[CaseInstanceTeamMemberTable]
          // Tasks for cases in which i belong to the case team ...
          .filter(_.caseInstanceId === tasks.caseInstanceId)
          // ... in an active membership
          .filter(_.active === true) // Only search in active team members
          // ... where any of my case roles matches the task's role - or where I am owner
          // (Note: if role in task is left empty, then it it still found because team members have also an empty role)
          .filter(member =>
            (
                (member.caseRole === tasks.role || member.isOwner === true)
                  &&
              // Now filter by either user id or tenant role (depending on the type of case team membership)
              ((member.isTenantUser === true && member.memberId === user.userId) || (member.isTenantUser === false && member.memberId === tenantMembership.role_name))
            )
          )
      } yield tasks
    }

    val query = for {
      baseQuery <- assignmentFilterQuery
        .filterOpt(filter.tenant)(_.tenant === _)
        .filterOpt(filter.taskState)(_.taskState === _)
        .filterOpt(filter.owner)(_.owner === _)
        .filterOpt(filter.dueOn)(_.dueDate >= getStartDate(_, filter.timeZone))
        .filterOpt(filter.dueOn)(_.dueDate <= getEndDate(_, filter.timeZone))
        .filterOpt(filter.dueBefore)(_.dueDate < getStartDate(_, filter.timeZone))
        .filterOpt(filter.dueAfter)(_.dueDate > getEndDate(_, filter.timeZone))

      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, filter.identifiers)

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

  override def getCountForUser(user: PlatformUser, tenant: Option[String]): Future[TaskCount] = {
    val claimedTasksQuery = TableQuery[TaskTable].filter(_.assignee === user.userId)
      .filterNot(_.taskState === "Completed")
      .filterNot(_.taskState === "Terminated")
      .filterOpt(tenant)(_.tenant === _)

    val unclaimedTasksQuery = for {
      // Select all unassigned tasks (optionally from the specified tenant)
      unclaimedTasks <- TableQuery[TaskTable].filter(_.assignee === "").filterOpt(tenant)(_.tenant === _)
      // In tenants where i am a user
      tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === unclaimedTasks.tenant)
      // Where my case team roles map to the task role
      tasksForMyCaseRoles <- TableQuery[CaseInstanceTeamMemberTable]
        // Tasks for cases in which i belong to the case team ...
        .filter(_.caseInstanceId === unclaimedTasks.caseInstanceId)
        // ... in an active membership
        .filter(_.active === true) // Only search in active team members
        // ... where any of my case roles matches the task's role - or where I am owner
        // (Note: if role in task is left empty, then it it still found because team members have also an empty role)
        .filter(member =>
          (
            (member.caseRole === unclaimedTasks.role || member.isOwner === true)
              &&
              // Now filter by either user id or tenant role (depending on the type of case team membership)
              ((member.isTenantUser === true && member.memberId === user.userId) || (member.isTenantUser === false && member.memberId === tenantMembership.role_name))
            )
        )
    } yield unclaimedTasks

    val count = for {
      claimTasks <- db.run(claimedTasksQuery.distinct.length.result)
      unclaimedTasks <- db.run(unclaimedTasksQuery.distinct.length.result)
    } yield (claimTasks, unclaimedTasks)

    count.map(c => {
      val tc = TaskCount(c._1, c._2)
      //      println("Returning Count: : " + tc)
      tc
    })
  }
}
