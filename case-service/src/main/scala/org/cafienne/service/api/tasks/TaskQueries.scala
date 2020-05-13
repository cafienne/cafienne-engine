package org.cafienne.service.api.tasks

import java.time.{Instant, LocalDateTime, ZoneOffset}

import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.service.api.Sort
import org.cafienne.service.api.cases.table.CaseTables
import org.cafienne.service.api.projection.TaskSearchFailure
import org.cafienne.service.api.tenant.TenantTables

import scala.concurrent.Future

case class TaskCount(claimed: Long, unclaimed: Long)

trait TaskQueries {


  // TODO: incorporate the casedefinition info, case parent id and case root id. For all queries. By joining.

  def getTask(taskId: String, user: PlatformUser): Future[Task] = ???

  def getCaseTypeTasks(caseType: String, tenant: Option[String], user: PlatformUser): Future[Seq[Task]] = ???

  def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[Task]] = ???

  def getCaseAndTenantInformation(taskId: String, user: PlatformUser): Future[Option[(String, String)]] = ???

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
                  timeZone: Option[String]): Future[Seq[Task]] = ???

  def getCountForUser(user: PlatformUser, tenant: Option[String]): Future[TaskCount] = ???
}


class TaskQueriesImpl extends TaskQueries
  with TaskTables
  with CaseTables
  with TenantTables {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val tasksQuery = TableQuery[TaskTable]

  // It will be really nice if this code can be similar to the optionFilter class
  private def tenants(optionalTenant: Option[String], user: PlatformUser): Seq[String] = {
    optionalTenant match {
      case Some(tenant) => Seq(tenant)
      case None => user.tenants
    }
  }

  override def getTask(taskId: String, user: PlatformUser): Future[Task] = {
    val query = TableQuery[TaskTable]
      .filter(_.id === taskId)
      .filter(_.tenant.inSet(user.tenants))
    db.run(query.result).map(t => if (t.isEmpty) throw TaskSearchFailure(taskId) else t.head)
  }

  override def getCaseTypeTasks(caseType: String, tenant: Option[String], user: PlatformUser): Future[Seq[Task]] = {
    val tenantSet = tenants(tenant, user)

    val task = TableQuery[TaskTable]
      .filter(_.tenant.inSet(tenantSet))
      .join(TableQuery[CaseInstanceTable]).on(_.caseInstanceId === _.id)
      .filter(_._2.definition === caseType)
    val tasks = db.run(task.result).map(records => records.map(record => record._1))
    tasks
  }

  override def getCaseTasks(caseInstanceId: String, user: PlatformUser): Future[Seq[Task]] = {
    val task = TableQuery[TaskTable]
      .filter(_.caseInstanceId === caseInstanceId)
      .filter(_.tenant.inSet(user.tenants))
    db.run(task.result)
  }

  override def getCaseAndTenantInformation(taskId: String, user: PlatformUser): Future[Option[(String, String)]] = {

    // SIMPLE test query to fetch all tenants in which the current user is enabled.
    //    val tenantsQ = usersTable.filter(_.id === user.id).filter(_.enabled === true).map(_.tenant)
    //    db.run(tenantsQ.result).map(records => {
    //      System.out.println("Found following tenants for user "+user.name)
    //      records.map(r => {
    //        System.out.println("Tenant: "+r)
    //      })
    //    })

    // LEFT JOIN on task and user; left join so that we know if the task exists and/or if the user exists and is enabled/disabled
    val taskUserInfo = for {
      (c, s) <- TableQuery[TaskTable].filter(_.id === taskId) joinLeft TableQuery[UserRoleTable].filter(_.userId === user.userId).filter(_.role_name === "") on (_.tenant === _.tenant)
    } yield (c.caseInstanceId, c.tenant, s.map(_.userId), s.map(_.enabled))

    db.run(taskUserInfo.result).map(r => r.headOption).map(u => u match {
      case Some((taskId: String, tenant: String, userId: Option[String], enabled: Option[Boolean])) => {
        (userId, enabled) match {
          case (None, None) => {
//            System.out.println("User does not even exist or not in this tenant")
            throw new SecurityException("User does not exist")
          }
          case (Some(id), Some(false)) => {
//            System.out.println("User "+id+" is disabled, cannot give the task")
            throw new SecurityException("User is not allowed to access task")
          }
          case (Some(id), Some(true)) => {
//            System.out.println("Everythign just fine; go ahead with your task user "+id)
            Some(taskId, tenant)
          }
          case (_, _) => throw new SecurityException("I should never reach this block of code")
        }
      }
      case None => {
        throw TaskSearchFailure(taskId)
      }
    })


//    val taskPlusUserInfo = TableQuery[TaskTable]
//      .join(TableQuery[UserTable])
//      .on(_.tenant === _.tenant)
//      .filter(_._1.id === taskId)
//      .filter(_._2.id === user.id)
//      .map(r => {
//        (r._1.caseInstanceId, r._2.id, r._2.enabled)
//
//      })
//    //      .filter(_._1.tenant.inSet(tenants))
//    //      .map((_._1.caseInstanceId, _._2.enabled)
//    //    val taskWithUser =
//    db.run(taskPlusUserInfo.result).map(records => records.headOption).map(u => u match {
//      case Some((taskId: String, userId: String, true)) => {
//        System.err.println("User "+userId+" is allowed to take action on task " +taskId)
//        Some(taskId)
//      }
//      case Some((taskId: String, userId: String, false)) => {
//        System.err.println("Not allowed since user "+userId+" is not allowed currently")
//        throw new SecurityException("Not allowed")
//      }
//      case None => {
//        System.err.println("Task cannot be found")
//        throw new SearchFailure("Task not found")
//      }
//    });

    //    val q = for {
    //      tenants <- usersTable.filter(_.id === user.id).filter(_.enabled === true).map(_.tenant).result
    //      tasks = TableQuery[TaskTable]
    //        .filter(_.id === taskId)
    //        .filter(_.tenant.inSet(tenants))
    //        .map(_.caseInstanceId)
    //    } yield (tenants, tasks)
    //
    //    db.run(q).map(x => {
    //      val f = x._1
    //      val t = x._2
    ////      System.err.println("We got an x: "+x)
    ////      System.err.println("We got an t: "+t.result)
    //      db.run(t.result).map(tResult => {
    //        val h = tResult.head
    ////        println("Katsjing katsjong: "+h)
    //      })
    //      x
    //    })
    //
    //    val caseInstanceId = TableQuery[TaskTable]
    //      .filter(_.id === taskId)
    //      .filter(_.tenant === user.tenant)
    //      .map(_.caseInstanceId)
    //    db.run(caseInstanceId.result).map(r => {
    //      System.out.println("R: "+r)
    //      r.headOption
    //    })
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
                           timeZone: Option[String]): Future[Seq[(Task)]] = {


    // First determine the set of tenants to query in (either the tenant passed into the method call, or all tenants that the user is member of)
    val tenantSet = tenants(tenant, user)

    // If there is no assignee given, then we need to query tasks that have a role that the user also has.
    //  Otherwise the query will not filter on roles
    val roles: Option[Traversable[String]] = assignee match {
      case None => {
        // TODO: this should become a join on task roles per user roles per tenant; currently any task in any role in any tenant that match together will be given...
        //  This is noted in issue https://github.com/cafienne/cafienne-engine/issues/14
        val rolesSet = user.users.filter(tenantUser => tenantSet.contains(tenantUser.tenant)).flatMap(tenantUser => tenantUser.roles)
        Some(rolesSet)
      }
      case _ => None
    }

    val query = TableQuery[TaskTable]
      .filter(_.tenant.inSet(tenantSet))
      .optionFilter(assignee)(_.assignee === _)
      .optionFilter(roles)((task, _) => task.role === "" || task.role.inSet(roles.getOrElse(Seq(""))))
      .optionFilter(taskState)(_.taskState === _)
      .optionFilter(owner)(_.owner === _)
      .optionFilter(assignee)(_.assignee === _)
      .optionFilter(dueOn)(_.dueDate >= getStartDate(_, timeZone))
      .optionFilter(dueOn)(_.dueDate <= getEndDate(_, timeZone))
      .optionFilter(dueBefore)(_.dueDate < getStartDate(_, timeZone))
      .optionFilter(dueAfter)(_.dueDate > getEndDate(_, timeZone))
      .distinctOn(_.caseInstanceId)
      .sortBy(getTasksSortField(_, sort.getOrElse(Sort("", None))))
      .drop(from).take(numOfResults)

    db.run(query.result)
      //          .map(records => {
      //            records.map(tuple => {
      //              //            println("Foudn task "+tuple.id)
      //              tuple
      //            })
      //          })
 }

  private def getTasksSortField(rep: (TaskTable), sort: Sort) = {
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
    val tasks = TableQuery[TaskTable]


    // We add the "empty" role to the user roles as well,
    //  in order to find matches on tasks where the role field is empty
//    val userRoles = user.roles().asScala + ""


    tenant match {
      case None => {
        val contexts = TableQuery[UserRoleTable]
        for {
          allClaimedTasks <- db.run(tasks
            .join(contexts).on(_.tenant === _.tenant)
            .filter(_._2.userId === user.userId)
            .filter(_._1.assignee === user.userId)
            .filterNot(_._1.taskState === "Completed")
            .filterNot(_._1.taskState === "Terminated")
            .distinctOn(_._1.id) // Apparently this does not help, we still get duplicates
            .result)
          allUnclaimedTasks <- db.run(tasks
            .join(contexts).on(_.tenant === _.tenant)
            .filter(_._2.userId === user.userId)
            .filter(_._1.taskState === "Unassigned")
            .distinctOn(_._1.id) // Apparently this does not help, we still get duplicates

// COMMENTED OUT FOR NOW, needs to become same complex join query as in get tasks
//            .filter(task => task._1.role.inSet(userRoles))
            .result)

        } yield {
          val claimedTaskIds = new java.util.ArrayList[String]()
          allClaimedTasks.filter(record => {
            val task = record._1
            claimedTaskIds.contains(task.id) match {
              case true => false
              case false => claimedTaskIds.add(task.id)
            }
          })

          val unclaimedTaskIds = new java.util.ArrayList[String]()
          allUnclaimedTasks.filter(record => {
            val task = record._1
            //            println("U-task " + task.taskName+": c["+task.tenant+"].id = "+task.id +", assigned to "+task.assignee+", roles: "+task.role)
            unclaimedTaskIds.contains(task.id) match {
              case true => false
              case false => unclaimedTaskIds.add(task.id)
            }
          })

          TaskCount(claimedTaskIds.size(), unclaimedTaskIds.size())
        }
      }
      case Some(tenant) => {
        val tenantRoles = user.getTenantUser(tenant).roles :+ ""
        for {
          claimedTasks <- db.run(tasks
            .filter(_.tenant === tenant)
            .filter(_.assignee === user.userId)
            .filterNot(_.taskState === "Completed")
            .filterNot(_.taskState === "Terminated")
            .length.result)
          unClaimedTasks <- db.run(tasks
            .filter(_.tenant === tenant)
            .filter(_.taskState === "Unassigned")
            .filter(task => task.role.inSet(tenantRoles))
            .length.result)
        } yield TaskCount(claimedTasks, unClaimedTasks)
      }
    }
  }
}
