package org.cafienne.service.api.tasks

import java.time.{Instant, LocalDateTime, ZoneOffset}

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.cmmn.instance.casefile.{LongValue, Value, ValueMap}
import org.cafienne.infrastructure.json.CafienneJson
import org.cafienne.service.api.Sort
import org.cafienne.service.api.cases.table.{CaseTables, CaseTeamMemberRecord}
import org.cafienne.service.api.projection.{CaseSearchFailure, TaskSearchFailure}
import org.cafienne.service.api.tenant.{TenantTables, UserRoleRecord}

import scala.concurrent.Future

case class TaskCount(claimed: Long, unclaimed: Long) extends CafienneJson {
  override def toValue: Value[_] = new ValueMap("claimed", new LongValue(claimed), "unclaimed", new LongValue(unclaimed))
}

trait TaskQueries {


  // TODO: incorporate the casedefinition info, case parent id and case root id. For all queries. By joining.

  def getTask(taskId: String, user: PlatformUser): Future[TaskRecord] = ???

  def getCaseTypeTasks(caseType: String, tenant: Option[String], user: PlatformUser): Future[Seq[TaskRecord]] = ???

  def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[TaskRecord]] = ???

  def authorizeTaskAccessAndReturnCaseAndTenantId(taskId: String, user: PlatformUser): Future[(String, String)] = ???

  def getAllTasks(tenant: Option[String],
                  caseDefinition: Option[String],
                  taskState: Option[String],
                  assignee: Option[String],
                  owner: Option[String],
                  dueOn: Option[String],
                  dueBefore: Option[String],
                  dueAfter: Option[String],
                  sort: Option[Sort],
                  from: Int = 0,
                  numOfResults: Int = 100,
                  user: PlatformUser,
                  timeZone: Option[String]): Future[Seq[TaskRecord]] = ???

  def getCountForUser(user: PlatformUser, tenant: Option[String]): Future[TaskCount] = ???
}


class TaskQueriesImpl extends TaskQueries
  with TaskTables
  with CaseTables
  with TenantTables
  with LazyLogging {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val tasksQuery = TableQuery[TaskTable]

  override def getTask(taskId: String, user: PlatformUser): Future[TaskRecord] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.id === taskId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.result.headOption).map{
      case Some(task) => task
      case None => throw TaskSearchFailure(taskId)
    }
  }

  override def getCaseTypeTasks(caseType: String, tenant: Option[String], user: PlatformUser): Future[Seq[TaskRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable]
        .join(TableQuery[CaseInstanceTable]).on(_.caseInstanceId === _.id)
        .filter(_._2.definition === caseType)
        .optionFilter(tenant)(_._1.tenant === _)
      // Access control query
      _ <- membershipQuery(user, baseQuery._1.caseInstanceId, baseQuery._1.tenant)
    } yield baseQuery._1

    db.run(query.distinct.result)
  }

  override def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[TaskRecord]] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.caseInstanceId === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      records
    })
  }

  /**
    * Query that validates that the user belongs to the team of the specified case, either by explicit
    * membership of the user id, or by one of the tenant roles of the user that are bound to the team of the case
    * @param user
    * @param caseInstanceId
    * @param tenant
    * @return
    */
  private def membershipQuery(user: PlatformUser, caseInstanceId: Rep[String], tenant: Rep[String]): Query[(UserRoleTable, CaseInstanceTeamMemberTable), (UserRoleRecord, CaseTeamMemberRecord), Seq] = {
    val query = for {
      // Validate tenant membership
      tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === tenant)
      // Validate case team membership: either user is explicit member or has a matching tenant role
      teamMembership <- TableQuery[CaseInstanceTeamMemberTable]
        .filter(_.caseInstanceId === caseInstanceId)
        .filter(_.active === true) // Only search in active team members
        .filter(_.caseRole === "") // Only search by base membership, not in certain roles
        .filter(member => { // Search by user id or by one of the user's tenant roles
          (member.isTenantUser === true && member.memberId === user.userId) ||
            (member.isTenantUser === false && member.memberId === tenantMembership.role_name)
        })
    } yield (tenantMembership, teamMembership)

    query
  }

  override def authorizeTaskAccessAndReturnCaseAndTenantId(taskId: String, user: PlatformUser): Future[(String, String)] = {
    val query = for {
      // Get the case
      baseQuery <- TableQuery[TaskTable].filter(_.id === taskId)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant)

    } yield (baseQuery.caseInstanceId, baseQuery.tenant)

    db.run(query.result.headOption).map{
      case None => throw TaskSearchFailure(taskId)
      case Some(result) => result
    }
  }

  override def getAllTasks(tenant: Option[String],
                           caseDefinition: Option[String],
                           taskState: Option[String],
                           assignee: Option[String],
                           owner: Option[String],
                           dueOn: Option[String],
                           dueBefore: Option[String],
                           dueAfter: Option[String],
                           sort: Option[Sort],
                           from: Int,
                           numOfResults: Int,
                           user: PlatformUser,
                           timeZone: Option[String]): Future[Seq[TaskRecord]] = {

    // If there is no assignee given, then we need to query tasks that have a role that the user also has.
    //  Otherwise the query will not filter on roles
    val assignmentFilterQuery = assignee match {
      case Some(assignee) => TableQuery[TaskTable].filter(_.assignee === assignee)
      case None => for {
        // Select all tasks
        tasks <- TableQuery[TaskTable]
        // In tenants where i am a user
        tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === tasks.tenant)
        // Where my case team roles map to the task role
        caseRoles <- TableQuery[CaseInstanceTeamMemberTable]
          // Tasks for cases in which i belong to the case team ...
          .filter(_.caseInstanceId === tasks.caseInstanceId)
          // ... in an active membership
          .filter(_.active === true) // Only search in active team members
          // ... where my case role matches the task's role ...
          // (Note: if role in task is left empty, then it it still found because team members have also an empty role)
          .filter(_.caseRole === tasks.role)
          // Now filter by either user id or tenant role (depending on the type of case team membership)
          .filter(member => { // Search by user id or by one of the user's tenant roles
            (member.isTenantUser === true && member.memberId === user.userId) ||
              (member.isTenantUser === false && member.memberId === tenantMembership.role_name)
          })
      } yield tasks
    }

    val query = for {
      baseQuery <- assignmentFilterQuery
        .optionFilter(tenant)(_.tenant === _)
        .optionFilter(taskState)(_.taskState === _)
        .optionFilter(owner)(_.owner === _)
        .optionFilter(dueOn)(_.dueDate >= getStartDate(_, timeZone))
        .optionFilter(dueOn)(_.dueDate <= getEndDate(_, timeZone))
        .optionFilter(dueBefore)(_.dueDate < getStartDate(_, timeZone))
        .optionFilter(dueAfter)(_.dueDate > getEndDate(_, timeZone))

        .drop(from).take(numOfResults)
      // Access control query
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant)

    } yield baseQuery

    db.run(query.sortBy(getTasksSortField(_, sort.getOrElse(Sort("", None)))).distinct.result)
  }

  private def getTasksSortField(rep: TaskTable, sort: Sort) = {
    val isAsc = sort.sortOrder exists {
      case s if s matches "(?i)asc" => true
      case _ => false
    }
    getSortBy(rep, sort.sortBy, isAsc)
  }

  private def getSortBy(rep: TaskTable, field: String, isAsc: Boolean) = field.toLowerCase match {
    case "taskstate" => if (isAsc) rep.taskState.asc else rep.taskState.desc
    case "assignee" => if (isAsc) rep.assignee.asc else rep.assignee.desc
    case "owner" => if (isAsc) rep.owner.asc else rep.owner.desc
    case "duedate" => if (isAsc) rep.dueDate.asc else rep.dueDate.desc
    case "createdon" => if (isAsc) rep.createdOn.asc else rep.createdOn.desc
    case "createdby" => if (isAsc) rep.createdBy.asc else rep.createdBy.desc
    case "modifiedby" => if (isAsc) rep.modifiedBy.asc else rep.modifiedBy.desc
    case "lastmodified" => if (isAsc) rep.lastModified.asc else rep.lastModified.desc
    case _ => if (isAsc) rep.lastModified.asc else rep.lastModified.desc
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
      .optionFilter(tenant)(_.tenant === _)

    val unclaimedTasksQuery = for {
      unclaimedTasks <- TableQuery[TaskTable].filter(_.assignee === "")
      // In tenants where i am a user
      tenantMembership <- TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.tenant === unclaimedTasks.tenant)
      // Where my case team roles map to the task role
      caseRoles <- TableQuery[CaseInstanceTeamMemberTable]
        // Tasks for cases in which i belong to the case team ...
        .filter(_.caseInstanceId === unclaimedTasks.caseInstanceId)
        // ... in an active membership
        .filter(_.active === true) // Only search in active team members
        // ... where my case role matches the task's role ...
        // (Note: if role in task is left empty, then it it still found because team members have also an empty role)
        .filter(_.caseRole === unclaimedTasks.role)
        // Now filter by either user id or tenant role (depending on the type of case team membership)
        .filter(member => { // Search by user id or by one of the user's tenant roles
          (member.isTenantUser === true && member.memberId === user.userId) ||
            (member.isTenantUser === false && member.memberId === tenantMembership.role_name)
        })
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
