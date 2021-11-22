package org.cafienne.timerservice

import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}

import java.time.Instant

case class Timer(caseInstanceId: String, timerId: String, moment: Instant, userId: String) {
  lazy val user: CaseUserIdentity = {
    new CaseUserIdentity {
      override val id: String = userId
      override val origin: Origin = Origin.TimerService
    }
  }
}

