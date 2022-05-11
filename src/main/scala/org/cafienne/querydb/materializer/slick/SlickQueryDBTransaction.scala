package org.cafienne.querydb.materializer.slick

import akka.Done
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.cmmn.instance.team.MemberType
import org.cafienne.infrastructure.cqrs.{OffsetRecord, OffsetStorage, OffsetStorageProvider}
import org.cafienne.infrastructure.jdbc.cqrs.{JDBCOffsetStorage, OffsetStoreTables}
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.materializer.cases.team.CaseTeamMemberKey
import org.cafienne.querydb.record._
import org.cafienne.querydb.schema.QueryDBSchema
import org.cafienne.querydb.schema.table.{CaseTables, ConsentGroupTables, TaskTables, TenantTables}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class SlickQueryDBTransaction
  extends QueryDBTransaction
    with QueryDBSchema
    with OffsetStorageProvider
    with CaseTables
    with TaskTables
    with TenantTables
    with ConsentGroupTables
    with OffsetStoreTables {

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val dbStatements: mutable.ListBuffer[DBIO[_]] = ListBuffer[DBIO[_]]()

  private lazy val meMyselfAndI_or_BasicallyThisOnly = this

  override def storage(name: String): OffsetStorage = new JDBCOffsetStorage {
    override val storageName: String = name
    override lazy val dbConfig = meMyselfAndI_or_BasicallyThisOnly.dbConfig
    override implicit val ec: ExecutionContext = meMyselfAndI_or_BasicallyThisOnly.ec
  }

  override def upsert(record: AnyRef): Unit = {
    if (record != null) {
      val upsertStatement = record match {
        case value: TaskRecord => TableQuery[TaskTable].insertOrUpdate(value)
        case value: PlanItemRecord => TableQuery[PlanItemTable].insertOrUpdate(value)
        case value: PlanItemHistoryRecord => TableQuery[PlanItemHistoryTable].insertOrUpdate(value)
        case value: CaseRecord => TableQuery[CaseInstanceTable].insertOrUpdate(value)
        case value: CaseDefinitionRecord => TableQuery[CaseInstanceDefinitionTable].insertOrUpdate(value)
        case value: CaseFileRecord => TableQuery[CaseFileTable].insertOrUpdate(value)
        case value: CaseBusinessIdentifierRecord => TableQuery[CaseBusinessIdentifierTable].insertOrUpdate(value)
        case value: CaseRoleRecord => TableQuery[CaseInstanceRoleTable].insertOrUpdate(value)
        case value: CaseTeamUserRecord => TableQuery[CaseInstanceTeamUserTable].insertOrUpdate(value)
        case value: CaseTeamTenantRoleRecord => TableQuery[CaseInstanceTeamTenantRoleTable].insertOrUpdate(value)
        case value: CaseTeamGroupRecord => TableQuery[CaseInstanceTeamGroupTable].insertOrUpdate(value)
        case value: OffsetRecord => TableQuery[OffsetStoreTable].insertOrUpdate(value)
        case value: UserRoleRecord => TableQuery[UserRoleTable].insertOrUpdate(value)
        case value: TenantRecord => TableQuery[TenantTable].insertOrUpdate(value)
        case value: ConsentGroupRecord => TableQuery[ConsentGroupTable].insertOrUpdate(value)
        case value: ConsentGroupMemberRecord => TableQuery[ConsentGroupMemberTable].insertOrUpdate(value)
        case other => throw new IllegalArgumentException("Upsert not supported for objects of type " + other.getClass.getName)
      }
      addStatement(upsertStatement)
    }
  }

  override def deleteTaskRecord(taskId: String): Unit = {
    addStatement(TableQuery[TaskTable].filter(_.id === taskId).delete)
  }

  override def deleteTenantUser(user: TenantUser): Unit = {
    addStatement(TableQuery[UserRoleTable].filter(userRoleRecord => userRoleRecord.userId === user.id && userRoleRecord.tenant === user.tenant).delete)
  }

  override def deleteCaseTeamMember(key: CaseTeamMemberKey): Unit = {
    key.memberType match {
      case MemberType.User => addStatement(TableQuery[CaseInstanceTeamUserTable].filter(_.caseInstanceId === key.caseInstanceId).filter(_.userId === key.memberId).delete)
      case MemberType.Group => addStatement(TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === key.caseInstanceId).filter(_.groupId === key.memberId).delete)
      case MemberType.TenantRole => addStatement(TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === key.caseInstanceId).filter(_.tenantRole === key.memberId).delete)
    }
  }

  override def deletePlanItemRecordAndHistory(planItemId: String): Unit = {
    addStatement(TableQuery[PlanItemTable].filter(_.id === planItemId).delete)
    addStatement(TableQuery[PlanItemHistoryTable].filter(_.id === planItemId).delete)
  }

  override def delete(record: AnyRef): Unit = {
    if (record != null) {
      val upsertStatement = record match {
        case value: TaskRecord => TableQuery[TaskTable].filter(_.id === value.id).delete
        case value: PlanItemRecord => TableQuery[PlanItemTable].filter(_.id === value.id).delete
        case value: PlanItemHistoryRecord => TableQuery[PlanItemHistoryTable].filter(_.id === value.id).delete
        case value: CaseRecord => TableQuery[CaseInstanceTable].filter(_.id === value.id).delete
        case value: CaseDefinitionRecord => TableQuery[CaseInstanceDefinitionTable].filter(_.caseInstanceId === value.caseInstanceId).delete
        case value: CaseFileRecord => TableQuery[CaseFileTable].filter(_.caseInstanceId === value.caseInstanceId).delete
        case value: CaseBusinessIdentifierRecord => TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === value.caseInstanceId).filter(_.name === value.name).delete
        case value: CaseRoleRecord => TableQuery[CaseInstanceRoleTable].filter(_.caseInstanceId === value.caseInstanceId).filter(_.roleName === value.roleName).delete
        case value: CaseTeamUserRecord => TableQuery[CaseInstanceTeamUserTable]
          .filter(_.caseInstanceId === value.caseInstanceId)
          .filter(_.userId === value.userId)
          .filter(_.caseRole === value.caseRole)
          .delete
        case value: CaseTeamTenantRoleRecord => TableQuery[CaseInstanceTeamTenantRoleTable]
          .filter(_.caseInstanceId === value.caseInstanceId)
          .filter(_.tenant === value.tenant)
          .filter(_.tenantRole === value.tenantRole)
          .filter(_.caseRole === value.caseRole)
          .delete
        case value: CaseTeamGroupRecord => TableQuery[CaseInstanceTeamGroupTable]
          .filter(_.caseInstanceId === value.caseInstanceId)
          .filter(_.groupId === value.groupId)
          .filter(_.groupRole === value.groupRole)
          .filter(_.caseRole === value.caseRole)
          .delete
        case value: UserRoleRecord => TableQuery[UserRoleTable].filter(r => r.userId === value.userId && r.tenant === value.tenant && r.role_name === value.role_name).delete
        case value: ConsentGroupRecord => TableQuery[ConsentGroupTable].filter(_.id === value.id).delete
        case value: ConsentGroupMemberRecord => TableQuery[ConsentGroupMemberTable].filter(_.group === value.group).filter(_.userId === value.userId).filter(_.role === value.role).delete
        case other => throw new IllegalArgumentException("Delete not supported for objects of type " + other.getClass.getName)
      }
      addStatement(upsertStatement)
    }
  }

  override def deleteConsentGroupMember(groupId: String, userId: String): Unit = {
    addStatement(TableQuery[ConsentGroupMemberTable].filter(_.group === groupId).filter(_.userId === userId).delete)
  }

  def addStatement(action: dbConfig.profile.api.DBIO[_]): Unit = {
    dbStatements += action
  }

  override def removeCaseRoles(caseInstanceId: String): Unit = {
    addStatement(TableQuery[CaseInstanceRoleTable].filter(_.caseInstanceId === caseInstanceId).delete)
  }

  def commit(): Future[Done] = {
    val transaction = dbStatements.toSeq
    // Clear statement buffer (the "transaction")
    dbStatements.clear()

    // Run the actions
    db.run(DBIO.sequence(transaction).transactionally).map { _ => Done }
  }

  override def getPlanItem(planItemId: String): Future[Option[PlanItemRecord]] = {
    db.run(TableQuery[PlanItemTable].filter(_.id === planItemId).result.headOption)
  }

  override def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]] = {
    db.run(TableQuery[UserRoleTable].filter(record => record.userId === key.userId && record.tenant === key.tenant && record.role_name === key.role_name).result.headOption)
  }

  override def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done] = {
    // Update logic has some complexity when the multiple old user id's are mapped to the same new user id
    //  In that case, duplicate key insertion may occur with the earlier approach that is done through 'simpleUpdate' below.
    val simpleUpdate = info.filter(u => u.newUserId != u.existingUserId).map(user => {
      (for {c <- TableQuery[UserRoleTable].filter(r => r.userId === user.existingUserId && r.tenant === tenant)} yield c.userId).update(user.newUserId)
    })

    val infoPerNewUserId: Set[(String, Set[String])] = convertUserUpdate(info)
    val hasNoDuplicates = !infoPerNewUserId.exists(update => update._2.size <= 1)

    // If there are no updates on different user id's to one new user id, then the update is simple

    val statements = if (hasNoDuplicates) {
      Future.successful(simpleUpdate)
    } else {
      val oldUserIds = info.map(_.existingUserId).toSet
      val allOldUsers = TableQuery[UserRoleTable].filter(r => r.tenant === tenant && r.userId.inSet(oldUserIds))
      val sql = db.run(allOldUsers.result).flatMap(records => {
        if (records.nonEmpty) {
          val deleteOldUsers = allOldUsers.delete
          val insertNewUsers = {
            infoPerNewUserId.flatMap(member => {
              val newMemberId = member._1

              val updatableRecords = records.filter(record => member._2.contains(record.userId))
              val userRecords = updatableRecords.filter(_.role_name.isBlank)
              val roleRecords = updatableRecords.filterNot(_.role_name.isBlank)

              // First user's name and email are taken as the "truth"; note: if there is no user record, a blank name and email are given
              val name = userRecords.headOption.fold("")(_.name)
              val email = userRecords.headOption.fold("")(_.email)
              val isOwner = userRecords.filter(_.enabled).filter(_.isOwner).toSet.nonEmpty
              val accountIsEnabled = userRecords.filter(_.enabled).toSet.nonEmpty

              val distinctActiveRoles = roleRecords.filter(_.enabled).map(_.role_name).toSet
              val distinctInactiveRoles = roleRecords.filterNot(_.enabled).filterNot(m => distinctActiveRoles.contains(m.role_name)).map(_.role_name).toSet

              val newUsersAndRoles: Seq[UserRoleRecord] = {
                // New user record
                Seq(UserRoleRecord(newMemberId, tenant, role_name = "", name = name, email = email, isOwner = isOwner, enabled = accountIsEnabled)) ++
                  // Active roles of the user
                  distinctActiveRoles.map(roleName => UserRoleRecord(newMemberId, tenant, role_name = roleName, name = "", email = "", isOwner = false, enabled = true)) ++
                  // Inactive roles of the user
                  distinctInactiveRoles.map(roleName => UserRoleRecord(newMemberId, tenant, role_name = roleName, name = "", email = "", isOwner = false, enabled = false))
              }
              newUsersAndRoles.map(record => TableQuery[UserRoleTable].insertOrUpdate(record))
            })
          }
          Future.successful(Seq(deleteOldUsers) ++ insertNewUsers)
        } else {
          // If there are no records, then we can simply use the old statement. Actually - do we even need to do anything?
          Future.successful(simpleUpdate)
        }
      })
      sql
    }

    statements.flatMap(sql => db.run(DBIO.sequence(sql ++ addOffsetRecord(offset)).transactionally).map(_ => Done))
  }

  private def convertUserUpdate(info: Seq[NewUserInformation]): Set[(String, Set[String])] = {
    val newUserIds: Set[String] = info.map(_.newUserId).toSet
    newUserIds.map(newUserId => (newUserId, info.filter(_.newUserId == newUserId).map(_.existingUserId).toSet))
  }

  //  var nr = 0L
  private def addOffsetRecord(offset: OffsetRecord): Seq[DBIO[_]] = {
    //    println(s"$nr: Updating $offsetName to $offset")
    //    nr += 1
    Seq(TableQuery[OffsetStoreTable].insertOrUpdate(offset))
  }

  def updateCaseUserInformation(caseId: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done] = {
    // When the user id is updated, then all records relating to the case instance need to be updated
    //  - Case itself on [createdBy, modifiedBy]
    //  - CaseDefinition on [modifiedBy]
    //  - PlanItem on [createdBy, modifiedBy]
    //  - PlanItemHistory on [modifiedBy]
    //  - Tasks on [createdBy, modifiedBy, assignee, owner]
    //  - CaseTeam members --> this has special logic, as multiple old id's may map to one new id.
    //    Updating this requires a database query, therefore this is isolated in a separate method returning a Future
    val updateQueries = info.map(user => {
      // Update 'createdBy' field in case instance table
      (for {cases <- TableQuery[CaseInstanceTable].filter(r => r.id === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in case instance table
      (for {cases <- TableQuery[CaseInstanceTable].filter(r => r.id === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in case instance definition table
      (for {cases <- TableQuery[CaseInstanceDefinitionTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'createdBy' field in planitem table
      (for {cases <- TableQuery[PlanItemTable].filter(r => r.caseInstanceId === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in planitem table
      (for {cases <- TableQuery[PlanItemTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in planitemhistory table
      (for {cases <- TableQuery[PlanItemHistoryTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'createdBy' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.createdBy === user.existingUserId)} yield cases.createdBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'modifiedBy' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.modifiedBy === user.existingUserId)} yield cases.modifiedBy).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'assignee' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.assignee === user.existingUserId)} yield cases.assignee).update(user.newUserId)
    }) ++ info.map(user => {
      // Update 'owner' field in task table
      (for {cases <- TableQuery[TaskTable].filter(r => r.caseInstanceId === caseId && r.owner === user.existingUserId)} yield cases.owner).update(user.newUserId)
    }) ++ addOffsetRecord(offset)

    // Now retrieve the future with the case team updates, and combine it with the above statements and run it
    val caseteamUpdates = constructCaseTeamUserIdUpdates(caseId, info)
    caseteamUpdates.flatMap(sql => {
      db.run(DBIO.sequence(updateQueries ++ sql).transactionally).map(_ => {
        //        println(s"---- updated case $caseId")
        Done
      })
    })
  }

  private def constructCaseTeamUserIdUpdates(caseId: String, info: Seq[NewUserInformation]): Future[Seq[DBIO[_]]] = {
    val infoPerNewUserId: Set[(String, Set[String])] = convertUserUpdate(info)
    val hasNoDuplicates = !infoPerNewUserId.exists(update => update._2.size <= 1)

    // If there are no duplicates (i.e., no situations where multiple old user id's map to the same new id)
    //  then the update is simple.
    // Otherwise we need to analyze the current database information (see below)
    val simpleCaseTeamUpdate = info.map(user => {
      // Update 'memberId' field in team table
      val member = for {
        member <- TableQuery[CaseInstanceTeamUserTable].filter(r => r.caseInstanceId === caseId && r.userId === user.existingUserId)
      } yield member.userId
      member.update(user.newUserId)
    })

    if (hasNoDuplicates) {
      Future.successful(simpleCaseTeamUpdate)
    } else {
      // Run a query on the old user id's, in order to find out what their role in the case team is
      //  - Subsequently delete all those old records, and replace them with a new set
      val oldUserIds = info.map(_.existingUserId).toSet
      val allOldMembers = TableQuery[CaseInstanceTeamUserTable].filter(r => r.caseInstanceId === caseId && r.userId.inSet(oldUserIds))
      val sql = db.run(allOldMembers.result).flatMap(records => {
        if (records.nonEmpty) {
          val tenant = records.head.tenant
          val deleteOldMembers = allOldMembers.delete
          val insertNewMembers = {
            infoPerNewUserId.flatMap(member => {
              val newUserId = member._1
              val updatableRecords = records.filter(record => member._2.contains(record.userId))
              val userRecords = updatableRecords.filter(_.caseRole.isBlank)
              val roleRecords = updatableRecords.filterNot(_.caseRole.isBlank)

              val distinctActiveRoles = roleRecords.map(_.caseRole).toSet

              // The new member becomes case owner if one or more of the old members is also case owner
              val isCaseOwner = userRecords.filter(_.isOwner).toSet.nonEmpty

              val newMembers: Seq[CaseTeamUserRecord] = {
                Seq(CaseTeamUserRecord(caseInstanceId = caseId, tenant = tenant, userId = newUserId, origin = "tenant", caseRole = "", isOwner = isCaseOwner)) ++
                  distinctActiveRoles.map(caseRole => CaseTeamUserRecord(caseInstanceId = caseId, tenant = tenant, userId = newUserId, origin = "tenant", caseRole = caseRole, isOwner = isCaseOwner))
              }
              newMembers.map(newMember => TableQuery[CaseInstanceTeamUserTable].insertOrUpdate(newMember))
            })
          }
          Future.successful(Seq(deleteOldMembers) ++ insertNewMembers)
        } else {
          Future.successful(simpleCaseTeamUpdate)
        }
      })
      sql
    }
  }

  override def getCaseInstance(id: String): Future[Option[CaseRecord]] = {
    db.run(TableQuery[CaseInstanceTable].filter(_.id === id).result.headOption)
  }

  override def getCaseFile(caseInstanceId: String): Future[Option[CaseFileRecord]] = {
    db.run(TableQuery[CaseFileTable].filter(_.caseInstanceId === caseInstanceId).result.headOption)
  }

  override def getTask(taskId: String): Future[Option[TaskRecord]] = {
    db.run(TableQuery[TaskTable].filter(_.id === taskId).result.headOption)
  }
}
