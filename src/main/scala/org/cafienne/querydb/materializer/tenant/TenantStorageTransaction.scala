package org.cafienne.querydb.materializer.tenant

import akka.Done
import org.cafienne.actormodel.identity.TenantUser
import org.cafienne.cmmn.actorapi.command.platform.NewUserInformation
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.record.{TenantRecord, UserRoleKey, UserRoleRecord}

import scala.concurrent.Future

trait TenantStorageTransaction extends QueryDBTransaction {
  def upsert(record: TenantRecord): Unit

  def upsert(record: UserRoleRecord): Unit

  def delete(record: UserRoleRecord): Unit

  def deleteTenantUser(user: TenantUser): Unit

  def getUserRole(key: UserRoleKey): Future[Option[UserRoleRecord]]

  def updateTenantUserInformation(tenant: String, info: Seq[NewUserInformation], offset: OffsetRecord): Future[Done]
}
