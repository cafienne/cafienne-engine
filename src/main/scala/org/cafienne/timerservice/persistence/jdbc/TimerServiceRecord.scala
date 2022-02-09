package org.cafienne.timerservice.persistence.jdbc

import java.time.Instant

final case class TimerServiceRecord(timerId: String, caseInstanceId: String, moment: Instant, tenant: String, user: String)
