package org.cafienne.service.db.query

import org.cafienne.actormodel.identity.PlatformUser
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.json.{CafienneJson, LongValue, Value, ValueMap}
import org.cafienne.service.db.query.exception.{CaseSearchFailure, TaskSearchFailure}
import org.cafienne.service.db.query.filter.TaskFilter
import org.cafienne.service.db.record.TaskRecord

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.Future

case class TaskCount(claimed: Long, unclaimed: Long) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap("claimed", new LongValue(claimed), "unclaimed", new LongValue(unclaimed))
}

trait TaskQueries {

  def getCaseMembership(taskId: String, user: PlatformUser): Future[CaseMembership] = ???

  def getTask(taskId: String, user: PlatformUser): Future[TaskRecord] = ???

  def getTasksWithCaseName(caseName: String, tenant: Option[String], user: PlatformUser): Future[Seq[TaskRecord]] = ???

  def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[TaskRecord]] = ???

  def getAllTasks(user: PlatformUser, filter: TaskFilter = TaskFilter.Empty, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[Seq[TaskRecord]] = ???

  def getCountForUser(user: PlatformUser, tenant: Option[String]): Future[TaskCount] = ???
}


class TaskQueriesImpl extends TaskQueries
  with BaseQueryImpl {

  import dbConfig.profile.api._

  val tasksQuery = TableQuery[TaskTable]

  override def getCaseMembership(taskId: String, user: PlatformUser): Future[CaseMembership] = {
    val tenantRoleBasedMembership =
      TableQuery[CaseInstanceTeamMemberTable].filter(_.isTenantUser === false)
        .join(TableQuery[TaskTable].filter(_.id === taskId))
        .on(_.caseInstanceId === _.caseInstanceId)
        .joinRight(TableQuery[UserRoleTable].filter(_.userId === user.id))
        .on((left, right) => left._1.memberId === right.role_name && left._1.tenant === right.tenant)
        .map(record => {
          (record._1.map(_._1.caseInstanceId), record._1.map(_._1.tenant), record._1.map(_._1.memberId))
        })

    val userIdBasedMembership = TableQuery[CaseInstanceTeamMemberTable]
      .filter(_.isTenantUser === true)
      .filter(_.memberId === user.id)
      .join(TableQuery[TaskTable].filter(_.id === taskId))
      .on(_.caseInstanceId === _.caseInstanceId)
      .map(user => (user._1.caseInstanceId, user._1.tenant, user._1.memberId))

    val query = userIdBasedMembership
      .joinFull(tenantRoleBasedMembership)

    db.run(query.result).map(records => {
      if (records.isEmpty) {
        throw TaskSearchFailure(taskId)
      }

      val userRecords: Set[(String, String, String)] = records.map(_._1).filter(_.nonEmpty).map(_.get).toSet
      val tenantRoleRecords: Set[(String, String, String)] = {
        // Convert Seq[Option[(Option[String], Option[String], Option[String])])] to Set(String, String, String)
        //  can it be done more elegantly? Now we're assessing that if the first string is non empty, it means the record is filled, otherwise not.
        //  Note: first string is case instance id. Would be pretty weird if that is not filled.
        val x: Seq[(Option[String], Option[String], Option[String])] = records.flatMap(_._2).filter(_._1.nonEmpty)
        val z = x.map(role => (role._1.get, role._2.getOrElse(""), role._3.getOrElse(""))).toSet
        z
      }

      createCaseUserIdentity(user, userRecords, tenantRoleRecords, TaskSearchFailure, taskId)
    })
  }

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

  override def getAllTasks(user: PlatformUser, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {

    // If there is no assignee given, then we need to query tasks that have a role that the user also has.
    //  Otherwise the query will not filter on roles
    val assignmentFilterQuery = filter.assignee match {
      case Some(assignee) => TableQuery[TaskTable].filter(_.assignee === assignee)
      case None => for {
        // Select all tasks
        tasks <- TableQuery[TaskTable]
        // In tenants where i am a user
        tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.id).filter(_.tenant === tasks.tenant)
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
              ((member.isTenantUser === true && member.memberId === user.id) || (member.isTenantUser === false && member.memberId === tenantMembership.role_name))
            )
          )
      } yield tasks
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
    val claimedTasksQuery = TableQuery[TaskTable].filter(_.assignee === user.id)
      .filterNot(_.taskState === "Completed")
      .filterNot(_.taskState === "Terminated")
      .filterOpt(tenant)(_.tenant === _)

    val unclaimedTasksQuery = for {
      // Select all unassigned tasks (optionally from the specified tenant)
      unclaimedTasks <- TableQuery[TaskTable].filter(_.assignee === "").filterOpt(tenant)(_.tenant === _)
        // And only active tasks, not completed or terminated
        .filterNot(_.taskState === "Completed").filterNot(_.taskState === "Terminated")
      // In tenants where i am a user
      tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.id).filter(_.tenant === unclaimedTasks.tenant)
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
              ((member.isTenantUser === true && member.memberId === user.id) || (member.isTenantUser === false && member.memberId === tenantMembership.role_name))
            )
        )
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
}
