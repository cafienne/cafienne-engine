package org.cafienne.actormodel

import org.apache.pekko.actor.Cancellable

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

/**
 * ModelActorMonitor provides a mechanism to have the ModelActor remove itself from memory after a specific idle period.
 */
class ModelActorMonitor(val actor: ModelActor) {
  private var selfCleaner: Option[Cancellable] = None

  def setBusy(): Unit = {
    selfCleaner.foreach(c => c.cancel())
  }

  def setFree(): Unit = {
    if (actor.hasAutoShutdown) {
      // Now set the new selfCleaner
      val idlePeriod = actor.caseSystem.config.actor.idlePeriod
      val duration = Duration.create(idlePeriod, TimeUnit.MILLISECONDS)
      selfCleaner = Some(actor.getScheduler.schedule(duration, () => actor.takeABreak()))
    }
  }
}
