package org.cafienne.timerservice

import org.cafienne.actormodel.identity.TenantUser

import java.time.Instant

case class Timer(caseInstanceId: String, timerId: String, moment: Instant, user: TenantUser)

