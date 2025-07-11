package org.cafienne.persistence.querydb.query.cmmn.implementations

import org.cafienne.actormodel.identity.{Origin, UserIdentity}
import org.cafienne.engine.cmmn.actorapi.command.team.{CaseTeam, CaseTeamGroup, CaseTeamTenantRole, CaseTeamUser, GroupRoleMapping}
import org.cafienne.engine.cmmn.definition.CMMNElementDefinition
import org.cafienne.persistence.querydb.query.cmmn.CaseInstanceQueries
import org.cafienne.persistence.querydb.query.exception.{CaseSearchFailure, PlanItemSearchFailure}
import org.cafienne.persistence.querydb.query.result.{CaseFileDocumentation, CaseTeamResponse, Documentation, FullCase}
import org.cafienne.persistence.querydb.record.{CaseDefinitionRecord, CaseFileRecord, CaseRecord, CaseTeamGroupRecord, CaseTeamTenantRoleRecord, CaseTeamUserRecord, PerformableTaskRecord, PlanItemRecord, TaskRecord}
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.Future

class CaseInstanceQueriesImpl(queryDB: QueryDB)
  extends BaseQueryImpl(queryDB) with CaseInstanceQueries {

  import dbConfig.profile.api._

  override def getFullCaseInstance(caseInstanceId: String, user: UserIdentity): Future[FullCase] = {
    val result = for {
      caseInstance <- getCaseInstance(caseInstanceId, user)
      caseTeam <- getCaseTeam(caseInstanceId, user)
      caseFile <- db.run(caseFileQuery.filter(_.caseInstanceId === caseInstanceId).result.headOption)
      casePlan <- db.run(planItemTableQuery.filter(_.caseInstanceId === caseInstanceId).result)
      identifiers <- db.run(caseIdentifiersQuery.filter(_.caseInstanceId === caseInstanceId).filter(_.active === true).result)
    } yield (caseInstance, caseTeam, caseFile, casePlan, identifiers)

    result.map(x => x._1.fold(throw CaseSearchFailure(caseInstanceId))(caseRecord => FullCase(caseRecord, file = x._3, team = x._2, planitems = x._4, identifiers = x._5)))
  }

  override def getCaseInstance(caseInstanceId: String, user: UserIdentity): Future[Option[CaseRecord]] = {
    //    println(s"Getting case $caseInstanceId for user ${user.id}")
    val query = for {
      // Get the case
      baseQuery <- TableQuery[CaseInstanceTable].filter(_.id === caseInstanceId)
      // Access control query
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery
    //        println("QUERY:\n\n" + query.result.statements.mkString("\n")+"\n\n")

    db.run(query.result.headOption)
  }

  override def getCaseTeam(caseInstanceId: String, user: UserIdentity): Future[CaseTeamResponse] = {
    val usersQuery = TableQuery[CaseInstanceTeamUserTable].filter(_.caseInstanceId === caseInstanceId)
    val tenantRolesQuery = TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseInstanceId)
    val groupsQuery = TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId)

    (for {
      userRecords <- db.run(usersQuery.result)
      tenantRoleRecords <- db.run(tenantRolesQuery.result)
      groupRecords <- db.run(groupsQuery.result)
      roleRecords <- db.run(TableQuery[CaseInstanceRoleTable].filter(_.caseInstanceId === caseInstanceId).map(_.roleName).result)
      membership <- db.run(membershipQuery(user, caseInstanceId).result)
    } yield (userRecords, tenantRoleRecords, groupRecords, roleRecords, membership))
      .map(records => {
        if (records._5.isEmpty) {
          throw CaseSearchFailure(caseInstanceId)
        }
        val users = records._1
        val tenantRoles = records._2
        val groups = records._3
        val caseRoleRecords: Seq[String] = records._4

        val team = fillCaseTeam(users, tenantRoles, groups)
        val caseRoles = caseRoleRecords.filterNot(_.isBlank)
        val unassignedRoles = caseRoles.filter(caseRole => !users.exists(_.caseRole == caseRole) && !tenantRoles.exists(_.caseRole == caseRole) && !groups.exists(_.caseRole == caseRole))
        CaseTeamResponse(team, caseRoles = caseRoles, unassignedRoles = unassignedRoles)
      })
  }

  private def fillCaseTeam(userRecords: Seq[CaseTeamUserRecord], tenantRoleRecords: Seq[CaseTeamTenantRoleRecord], groupRecords: Seq[CaseTeamGroupRecord]): CaseTeam = {
    val userMembers = getUserMembers(userRecords)
    val tenantRoleMembers = getTenantRoleMembers(tenantRoleRecords)
    val groupIds: Set[String] = groupRecords.map(_.groupId).toSet
    val groups: Seq[CaseTeamGroup] =groupIds.map(groupId => {
      val mappings = groupRecords.filter(_.groupId == groupId)
      val groupRoles = mappings.map(_.groupRole).toSet
      val groupRoleMappings = groupRoles.map(groupRole => {
        val caseRoles = mappings.filter(_.groupRole == groupRole).map(_.caseRole).toSet.filterNot(_.isBlank)
        val isOwner = mappings.filter(_.groupRole == groupRole).exists(_.isOwner)
        GroupRoleMapping(groupRole = groupRole, isOwner = isOwner, caseRoles = caseRoles)
      })
      CaseTeamGroup(groupId, groupRoleMappings.toSeq)
    }).toSeq
    //    val groups: Seq[CaseTeamGroup] = groupIds.map(id => CaseTeamGroup(id, groupRecords.filter(_.groupId == id).map(m => GroupRoleMapping(m.caseRole, m.groupRole, m.isOwner)))).toSeq
    CaseTeam(users = userMembers, tenantRoles = tenantRoleMembers, groups = groups)
  }

  private def getUserMembers(userRecords: Seq[CaseTeamUserRecord]): Seq[CaseTeamUser] = {
    val users = userRecords.filter(record => record.caseRole.isBlank)
    val roles = userRecords.filterNot(record => record.caseRole.isBlank)
    users.map(user => CaseTeamUser.from(userId = user.userId, origin = Origin.getEnum(user.origin), isOwner = user.isOwner, caseRoles = roles.filter(role => role.userId == user.userId).map(_.caseRole).toSet))
  }

  private def getTenantRoleMembers(roleRecords: Seq[CaseTeamTenantRoleRecord]): Seq[CaseTeamTenantRole] = {
    val records = roleRecords.filter(record => record.caseRole.isBlank)
    val caseRoles = roleRecords.filterNot(record => record.caseRole.isBlank)
    records.map(tRole => CaseTeamTenantRole(tenantRoleName = tRole.tenantRole, isOwner = tRole.isOwner, caseRoles = caseRoles.filter(role => role.tenantRole == tRole.tenantRole).map(_.caseRole).toSet))
  }

  override def getCaseDefinition(caseInstanceId: String, user: UserIdentity): Future[CaseDefinitionRecord] = {
    val query = for {
      // Get the case file
      baseQuery <- caseDefinitionQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => result
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getCaseFile(caseInstanceId: String, user: UserIdentity): Future[CaseFileRecord] = {
    val query = for {
      // Get the case file
      baseQuery <- caseFileQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => result
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getCaseFileDocumentation(caseInstanceId: String, user: UserIdentity): Future[CaseFileDocumentation] = {
    val query = for {
      // Get the case file
      baseQuery <- caseDefinitionQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case Some(result) => CaseFileDocumentation(result)
      case None => throw CaseSearchFailure(caseInstanceId)
    }
  }

  override def getPlanItems(caseInstanceId: String, user: UserIdentity): Future[Seq[PlanItemRecord]] = {
    val query = for {
      // Get the items in the case plan
      baseQuery <- planItemTableQuery.filter(_.caseInstanceId === caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, caseInstanceId)
    } yield baseQuery

    db.run(query.distinct.result).map(records => {
      if (records.isEmpty) throw CaseSearchFailure(caseInstanceId)
      records
    })
  }

  override def getPlanItem(planItemId: String, user: UserIdentity): Future[PlanItemRecord] = {
    val query = for {
      // Get the item
      baseQuery <- planItemTableQuery.filter(_.id === planItemId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery

    db.run(query.result.headOption).map {
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) => record
    }
  }

  override def getPlanItemDocumentation(planItemId: String, user: UserIdentity): Future[Documentation] = {
    val query = for {
      // Get the item
      baseQuery <- planItemTableQuery.filter(_.id === planItemId)
      definitionQuery <- caseDefinitionQuery.filter(_.caseInstanceId === baseQuery.caseInstanceId)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield (baseQuery, definitionQuery)

    db.run(query.result.headOption).map {
      case None => throw PlanItemSearchFailure(planItemId)
      case Some(record) =>
        val definitionId = record._1.definitionId
        val definitionDocument = record._2.definitions
        val element: CMMNElementDefinition = definitionDocument.findElement(element => definitionId.equals(element.getId))
        element == null match {
          case true => Documentation("")
          case _ => Documentation(element.documentation.text, element.documentation.textFormat)
        }
    }
  }

  private case class CaseRoleAccess(caseInstanceId: String, caseRole: String, hasCaseOwnership: Boolean)

  override def getCaseTasks(caseInstanceId: String, user: UserIdentity, includeSubCaseTasks: Boolean): Future[Seq[PerformableTaskRecord]] = {
    //    println("\n================= USER " + user.id +" IS FETCHING TASKS OF " + caseInstanceId)

    // Below 2 queries could be re-usable. They enable fetching a chain of cases, once without authorization information, the other with authorization information
    val rootCaseChain = {
      if (includeSubCaseTasks) {
        for {
          root <- TableQuery[CaseIdentifierView].filter(_.id === caseInstanceId).map(_.rootCaseId)
          ids <- TableQuery[CaseIdentifierView].filter(_.rootCaseId === root)
        } yield ids
      } else {
        TableQuery[CaseIdentifierView].filter(_.id === caseInstanceId)
      }
    }

    val authorizedCaseChain = for {
      ids <- rootCaseChain
      access <- queryCaseAccess(user, ids.id)
    } yield access

    // We're joining 2 queries:
    //  1. on the one hand get the entire chain of cases to which the caseInstanceId belongs
    //  2. then also fetch all tasks for all those cases, but with full authorization information on the cases to which those tasks belong
    //  The second query helps us to understand that a case is accessible by the user, the first one is needed to find out which cases are a subcase of the requested caseInstanceId
    val tasksWithCaseAccess = authorizedCaseChain.distinct.joinFull(TableQuery[TaskTable]).on(_._1 === _.caseInstanceId)

    // Joining left, because the case tree must always exist, and if it doesn't we don't have access to it either and we'll throw case search failure.
    val query = rootCaseChain.joinFull(tasksWithCaseAccess).on(_.id === _._2.map(_.caseInstanceId))

    //      println("\n\nQuery\n" + query.result.statements.mkString("") + "\n\n")

    db.run(query.result).map(records => {
      //        val summary = records.map(record => "case["+record._1.map(_.id).getOrElse("NONE") + "]: " + record._2.map(_._1.fold("not authorized")(_ => "having access ")).getOrElse("   NO AUTH    ")  + "  |  " + record._2.map(_._2.map(_.taskName).getOrElse("no task here")).getOrElse(" NO TASKS"))
      //        println("\n\n" + user.id +" found " + records.length +" records for case " + caseInstanceId +":\n" + summary.mkString("\n"))
      val rootCaseChain = records.map(_._1)
      val tasksAndCasesWithAuthorization = records.map(_._2).filter(_.nonEmpty).map(_.get)
      val caseAccess: Seq[CaseRoleAccess] = tasksAndCasesWithAuthorization.map(_._1).filter(_.nonEmpty).map(_.get).map(r => CaseRoleAccess(r._1, r._2, r._3))
      val tasks: Seq[TaskRecord] = tasksAndCasesWithAuthorization.map(_._2).filter(_.nonEmpty).map(_.get).distinctBy(_.id)

      val accessibleCases = caseAccess
      if (caseAccess.isEmpty) {
        //          println("No accessible cases")
        throw CaseSearchFailure(caseInstanceId)
      } else if (!accessibleCases.exists(c => c.caseInstanceId == caseInstanceId)) {
        //          println("Root case requested not accessible")
        throw CaseSearchFailure(caseInstanceId)
      } else {

        // Mechanism to build a hierarchy of case and subcases
        case class CaseNode(id: String, parent: String, root: String) {
          import scala.collection.mutable.ListBuffer

          val isRoot: Boolean = id == root
          val subcases: ListBuffer[CaseNode] = new ListBuffer()
          def chain: Seq[CaseNode] = Seq(this) ++ this.subcases.flatMap(_.chain)
          //
          //    def print(indent: String = ""): Unit = {
          //      println(indent + id)
          //      subcases.foreach(_.print(indent + "  "))
          //    }
        }

        // First map the list of all cases with the same root case. If empty or it doesn't contain our case id, then the case does not exist
        val caseNodes = rootCaseChain.filter(_.nonEmpty).map(_.get).map(r => CaseNode(r.id, r.parentCaseId, r.rootCaseId)).distinct
        val caseNode = caseNodes.find(node => node.id == caseInstanceId).getOrElse({
          //            println("Case really not found")
          throw CaseSearchFailure(caseInstanceId)
        })

        // Now build a tree structure with all cases and their subcases, starting from the root case id
        caseNodes.foreach(node => caseNodes.find(parent => parent.id == node.parent).foreach(parent => parent.subcases += node))

        //          println("Found " + accessibleCases.length +" cases with access. Searching for tasks in subtree ")
        //          caseNode.print()

        val accessibleCaseInstanceNodes = caseNode.chain.filter(node => accessibleCases.exists(c => c.caseInstanceId == node.id))
        if (accessibleCaseInstanceNodes.isEmpty) {
          // The user does not have access to any of the cases in the case tree for the given caseInstanceId, hence giving search failure.
          //            println("No accessible cases in case tree")
          throw CaseSearchFailure(caseInstanceId)
        }

        // Now filter the tasks for the cases in the chain of the requested caseInstanceId
        val result = tasks.filter(task => accessibleCaseInstanceNodes.exists(c => c.id == task.caseInstanceId))
        def performable(task: TaskRecord): Boolean = caseAccess.exists(role => role.caseInstanceId == task.caseInstanceId && (role.caseRole == task.role || task.role.isEmpty || role.hasCaseOwnership))

        //          println("\n" + user.id +" found " + result.length +" tasks "+ result.map(r => (r.taskName , r.id, r.caseInstanceId)).mkString("\n"))
        result.map(task => PerformableTaskRecord(task, performable(task)))
      }
    })
  }
}
