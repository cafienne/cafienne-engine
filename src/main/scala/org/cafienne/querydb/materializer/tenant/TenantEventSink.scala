package org.cafienne.querydb.materializer.tenant

import akka.persistence.query.Offset
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.querydb.materializer.slick.{QueryDBEventSink, SlickQueryDBTransaction}
import org.cafienne.system.CaseSystem
import org.cafienne.tenant.actorapi.event.TenantEvent

import scala.concurrent.Future

class TenantEventSink(val caseSystem: CaseSystem) extends QueryDBEventSink with LazyLogging {
  override val tag: String = TenantEvent.TAG

  val persistence = new SlickQueryDBTransaction
  def offsetStorage: OffsetStorage = persistence.storage(TenantEventSink.offsetName)

  override def getOffset(): Future[Offset] = offsetStorage.getOffset

  override def createTransaction(envelope: ModelEventEnvelope): TenantTransaction = new TenantTransaction(envelope.persistenceId, persistence, caseSystem.userCache)
}

object TenantEventSink {
  val offsetName = "TenantEventSink"
}
