package org.cafienne.querydb.materializer.consentgroup

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.event.CommitEvent
import org.cafienne.consentgroup.actorapi.event.{ConsentGroupCreated, ConsentGroupMemberEvent, ConsentGroupModified}
import org.cafienne.infrastructure.akkahttp.authentication.IdentityProvider
import org.cafienne.infrastructure.cqrs.ModelEventEnvelope
import org.cafienne.infrastructure.cqrs.offset.OffsetRecord
import org.cafienne.querydb.materializer.slick.{SlickQueryDBTransaction, SlickTransaction}

import scala.concurrent.Future

class ConsentGroupTransaction(groupId: String, userCache: IdentityProvider) extends SlickTransaction with LazyLogging {
  import scala.concurrent.ExecutionContext.Implicits.global
  val persistence = new SlickQueryDBTransaction

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
    persistence.upsert(OffsetRecord(ConsentGroupEventSink.offsetName, envelope.offset))
    // Commit and then inform the last modified registration
    persistence.commit().andThen(_ => {
      memberProjection.affectedUserIds.foreach(userCache.clear)
      ConsentGroupReader.lastModifiedRegistration.handle(event)
    })
  }

}
