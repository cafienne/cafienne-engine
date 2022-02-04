package org.cafienne.querydb.materializer.cases.team

import akka.Done
import com.typesafe.scalalogging.LazyLogging
import org.cafienne.cmmn.actorapi.event.team._
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent
import org.cafienne.querydb.materializer.RecordsPersistence

import scala.concurrent.{ExecutionContext, Future}

class CaseTeamProjection(persistence: RecordsPersistence)(implicit val executionContext: ExecutionContext) extends LazyLogging {
  private val memberProjection = new CaseTeamMemberProjection(persistence)
  private val deprecatedEventsProjection = new DeprecatedCaseTeamEventProjection(persistence)

  def handleCaseTeamEvent(event: CaseTeamEvent): Future[Done] = {
    // We handle 2 types of event: either the old ones (which carried all info in one shot) or the new ones, which are more particular
    event match {
      case event: CaseTeamMemberEvent[_] => memberProjection.handleEvent(event)
      case event: DeprecatedCaseTeamEvent => deprecatedEventsProjection.handleDeprecatedCaseTeamEvent(event)
      case _ => // Ignore other events
    }
    Future.successful(Done)
  }

  def prepareCommit(): Unit = {
    memberProjection.prepareCommit()
    deprecatedEventsProjection.prepareCommit()
  }
}
