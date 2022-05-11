package org.cafienne.querydb.materializer.tenant

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.record.TenantRecord
import org.cafienne.tenant.actorapi.event.platform.{PlatformEvent, TenantCreated, TenantDisabled, TenantEnabled}

import scala.concurrent.{ExecutionContext, Future}

class TenantProjection(persistence: QueryDBTransaction)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val tenants = scala.collection.mutable.HashMap[String, TenantRecord]()


  def handlePlatformEvent(event: PlatformEvent): Future[Done] = {
    event match {
      case newTenant: TenantCreated => tenants.put(newTenant.tenantName, TenantRecord(newTenant.tenantName()))
      case disabledTenant: TenantDisabled => tenants.put(disabledTenant.tenantName(), TenantRecord(disabledTenant.tenantName(), enabled = false))
      case enabledTenant: TenantEnabled => tenants.put(enabledTenant.tenantName(), TenantRecord(enabledTenant.tenantName(), enabled = true))
      case _ => Future.successful(Done) // Ignore other events
    }
    Future.successful(Done)
  }

  def prepareCommit(): Unit = {
    this.tenants.values.foreach(instance => persistence.upsert(instance))
  }
}
