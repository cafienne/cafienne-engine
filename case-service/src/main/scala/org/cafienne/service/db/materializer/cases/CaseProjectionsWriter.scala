package org.cafienne.service.db.materializer.cases

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.CaseEvent
import org.cafienne.infrastructure.cqrs.{OffsetStorage, OffsetStorageProvider}
import org.cafienne.service.api.cases.CaseReader
import org.cafienne.service.db.materializer.slick.SlickEventMaterializer
import org.cafienne.service.db.materializer.{LastModifiedRegistration, RecordsPersistence}

class CaseProjectionsWriter(persistence: RecordsPersistence, offsetStorageProvider: OffsetStorageProvider)(implicit override val system: ActorSystem) extends SlickEventMaterializer with LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def offsetStorage: OffsetStorage = offsetStorageProvider.storage("CaseProjectionsWriter")
  override val tag: String = CaseEvent.TAG
  override val lastModifiedRegistration: LastModifiedRegistration = CaseReader.lastModifiedRegistration

  def createTransaction(caseInstanceId: String, tenant: String) = new CaseTransaction(caseInstanceId, tenant, persistence)

}
