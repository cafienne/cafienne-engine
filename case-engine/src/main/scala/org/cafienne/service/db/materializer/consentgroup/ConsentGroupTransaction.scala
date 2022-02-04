package org.cafienne.service.db.materializer.consentgroup

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.consentgroup.actorapi.event.{ConsentGroupCreated, ConsentGroupMemberEvent, ConsentGroupModified}
import org.cafienne.infrastructure.akka.http.authentication.IdentityProvider
import org.cafienne.infrastructure.cqrs.{ModelEventEnvelope, OffsetStorage}
import org.cafienne.service.db.materializer.RecordsPersistence
import org.cafienne.service.db.materializer.slick.SlickTransaction

import scala.concurrent.{ExecutionContext, Future}

class ConsentGroupTransaction(groupId: String, persistence: RecordsPersistence, userCache: IdentityProvider, offsetStorage: OffsetStorage)
                         (implicit val executionContext: ExecutionContext) extends SlickTransaction with LazyLogging {

  private val groupProjection = new GroupProjection(persistence)
  private val memberProjection = new GroupMemberProjection(groupId, persistence)

  def handleEvent(envelope: ModelEventEnvelope): Future[Done] = {
    envelope.event match {
      case event: ConsentGroupCreated => groupProjection.handleGroupEvent(event)
      case event: ConsentGroupMemberEvent => memberProjection.handleMemberEvent(event)
      case _ => Future.successful(Done) // Ignore other events
    }
  }

  override def commit(envelope: ModelEventEnvelope, transactionEvent: CommitEvent): Future[Done] = {
    transactionEvent match {
      case event: ConsentGroupModified => commitGroupRecords(envelope, event)
      case _ =>
        logger.warn(s"ConsentGroupTransaction unexpectedly receives a commit event of type ${transactionEvent.getClass.getName}. This event is ignored.")
        Future.successful(Done)
    }
  }

  private def commitGroupRecords(envelope: ModelEventEnvelope, event: ConsentGroupModified): Future[Done] = {
    // Add group and members (if any) to the db transaction
    groupProjection.prepareCommit()
    memberProjection.prepareCommit()
    // Update the offset of the last event handled in this projection
    persistence.upsert(offsetStorage.createOffsetRecord(envelope.offset))
    // Commit and then inform the last modified registration
    persistence.commit().andThen(_ => {
      memberProjection.affectedUserIds.foreach(userCache.clear)
      ConsentGroupReader.lastModifiedRegistration.handle(event)
    })
  }

}
