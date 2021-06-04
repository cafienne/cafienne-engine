package org.cafienne.timerservice

import org.cafienne.akka.actor.identity.TenantUser

import java.time.Instant

case class Timer(caseInstanceId: String, timerId: String, moment: Instant, user: TenantUser)

