package org.cafienne.querydb.query

import org.cafienne.actormodel.identity.PlatformUser
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
    val groupMembership = TableQuery[CaseInstanceTeamGroupTable]
      .join(TableQuery[TaskTable].filter(_.id === taskId))
      .on(_.caseInstanceId === _.caseInstanceId)
      .filter(_._1.groupId.inSet(user.groups.map(_.groupId)))
      .distinctOn(_._1.groupId)
      .map(group => (group._1.caseInstanceId, group._1.tenant, group._1.groupId))

    val tenantRoleBasedMembership =
      TableQuery[CaseInstanceTeamTenantRoleTable]
        .join(TableQuery[TaskTable].filter(_.id === taskId))
        .on(_.caseInstanceId === _.caseInstanceId)
        .joinRight(TableQuery[UserRoleTable].filter(_.userId === user.id))
        .on((left, right) => left._1.tenantRole === right.role_name && left._1.tenant === right.tenant)
        .map(record => {
          (record._1.map(_._1.caseInstanceId), record._1.map(_._1.tenant), record._1.map(_._1.tenantRole))
        })

    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === user.id)
      .join(TableQuery[TaskTable].filter(_.id === taskId))
      .on(_.caseInstanceId === _.caseInstanceId)
      .map(user => (user._1.caseInstanceId, user._1.tenant, user._1.userId))

    val query = userIdBasedMembership
      .joinFull(tenantRoleBasedMembership)
      .joinFull(groupMembership)

    db.run(query.result).map(records => {
      // Records have this signature:
      //      val _: Seq[]
      //      _1: Option[]
      //        _1._1 Option[(String, String, String)],
      //        _1._2 Option[(Option[String], Option[String], Option[String])]
      //      _2: Option[(String, String, String)])] = records
      if (records.isEmpty) throw TaskSearchFailure(taskId)

      val userRecords: Set[(String, String, String)] = records.map(_._1.map(_._1)).filter(_.nonEmpty).flatMap(_.get).toSet
      val tenantRoleRecords: Set[(String, String, String)] = {
        // Convert Seq[Option[(Option[String], Option[String], Option[String])])] to Set(String, String, String)
        //  can it be done more elegantly? Now we're assessing that if the first string is non empty, it means the record is filled, otherwise not.
        //  Note: first string is case instance id. Would be pretty weird if that is not filled.
        val x: Seq[Option[(Option[String], Option[String], Option[String])]] = records.map(_._1.flatMap(_._2))
        val y: Seq[(Option[String], Option[String], Option[String])] = x.filter(_.nonEmpty).map(_.get).filter(_._1.nonEmpty)
        val z =  y.map(role => (role._1.get, role._2.getOrElse(""), role._3.getOrElse(""))).toSet
        z
      }
      val groupRecords: Set[(String, String, String)] = records.map(_._2).filter(_.nonEmpty).map(_.get).toSet

      createCaseUserIdentity(user, userRecords, groupRecords, tenantRoleRecords, TaskSearchFailure, taskId)
    })
  }

  override def getTask(taskId: String, user: PlatformUser): Future[TaskRecord] = {
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

  override def getTasksWithCaseName(caseName: String, tenant: Option[String], user: PlatformUser): Future[Seq[TaskRecord]] = {
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

  override def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[TaskRecord]] = {
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

  override def getAllTasks(platformUser: PlatformUser, filter: TaskFilter, area: Area, sort: Sort): Future[Seq[TaskRecord]] = {
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
          consentGroupCoupledCaseRoles(platformUser).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || tenantRoleCoupledCaseRoles(platformUser).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || userCoupledCaseRoles(platformUser).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists)
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
      _ <- membershipQuery(platformUser, baseQuery.caseInstanceId, filter.identifiers)

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

  override def getCountForUser(platformUser: PlatformUser, tenant: Option[String]): Future[TaskCount] = {
    val claimedTasksQuery = TableQuery[TaskTable].filter(_.assignee === platformUser.id)
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
          consentGroupCoupledCaseRoles(platformUser).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || tenantRoleCoupledCaseRoles(platformUser).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists
            || userCoupledCaseRoles(platformUser).filter(task.caseInstanceId === _._1).filter(task.role === _._2).exists)
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
  private def consentGroupCoupledCaseRoles(platformUser: PlatformUser): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[ConsentGroupMemberTable].filter(_.userId === platformUser.id)
      .join(TableQuery[CaseInstanceTeamGroupTable]).on(_.role === _.groupRole).map(_._2)
      .map(group => (group.caseInstanceId, group.caseRole))
  }

  private def tenantRoleCoupledCaseRoles(platformUser: PlatformUser): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[UserRoleTable]
      .filter(_.userId === platformUser.id)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable])
      .on((left, right) => left.role_name === right.tenantRole && left.tenant === right.tenant)
      .map(_._2)
      .map(tenantRole => (tenantRole.caseInstanceId, tenantRole.caseRole))
  }

  private def userCoupledCaseRoles(platformUser: PlatformUser): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === platformUser.id)
      .map(user => (user.caseInstanceId, user.caseRole))
  }
}
