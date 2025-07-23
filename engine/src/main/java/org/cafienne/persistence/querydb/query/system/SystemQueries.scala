package org.cafienne.persistence.querydb.query.system

import org.cafienne.actormodel.ActorType
import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.querydb.query.cmmn.implementations.BaseQueryImpl
import org.cafienne.persistence.querydb.query.exception.ActorSearchFailure
import org.cafienne.persistence.querydb.schema.QueryDB
import org.cafienne.persistence.querydb.schema.table.{CaseTables, TenantTables}
import org.cafienne.service.storage.StorageUser
import org.cafienne.service.storage.actormodel.ActorMetadata

import scala.concurrent.Future

trait SystemQueries {
  def findActor(user: UserIdentity, actorId: String): Future[ActorMetadata]
}

class SystemQueriesImpl(queryDB: QueryDB) extends BaseQueryImpl(queryDB)
  with SystemQueries
  with CaseTables
  with TenantTables {

  import dbConfig.profile.api._

  override def findActor(user: UserIdentity, actorId: String): Future[ActorMetadata] = {
    val caseQuery = TableQuery[CaseInstanceTable].filter(_.id === actorId).map(_.rootCaseId)
    val processQuery = TableQuery[PlanItemTable].filter(_.id === actorId).join(TableQuery[CaseInstanceTable]).on(_.caseInstanceId === _.id).map(_._2.rootCaseId)
    val tenantQuery = TableQuery[TenantTable].filter(_.name === actorId).map(_.name)
    val consentGroupQuery = TableQuery[ConsentGroupTable].filter(_.id === actorId).map(_.id)

    val query = caseQuery.joinFull(processQuery.joinFull(tenantQuery.joinFull(consentGroupQuery)))
    db.run(query.result).map(records => {
      if (records.isEmpty) {
        throw ActorSearchFailure(actorId)
      }
      val usr = StorageUser(user.id, "")
      val caseInstance = records.map(_._1).filter(_.nonEmpty).map(_.get).headOption
      if (caseInstance.nonEmpty) {
        ActorMetadata(usr, ActorType.Case, actorId)
      } else {
        val processInstance = records.map(_._2).filter(_.nonEmpty).map(_.get).filter(_._1.nonEmpty).map(_._1.get).headOption
        if (processInstance.nonEmpty) {
          ActorMetadata(usr, ActorType.Process, actorId)
        } else {
          val tenantsAndGroups = records.map(_._2).filter(_.nonEmpty).map(_.get).filter(_._2.nonEmpty).map(_._2.get)
          val tenant = tenantsAndGroups.map(_._1).filter(_.nonEmpty).map(_.get).headOption
          if (tenant.nonEmpty) {
            ActorMetadata(usr, ActorType.Tenant, actorId)
          } else {
            val group = tenantsAndGroups.map(_._2).filter(_.nonEmpty).map(_.get).headOption
            if (group.nonEmpty) {
              ActorMetadata(usr, ActorType.Group, actorId)
            } else {
              throw ActorSearchFailure(actorId)
            }
          }
        }
      }
    })
  }
}
