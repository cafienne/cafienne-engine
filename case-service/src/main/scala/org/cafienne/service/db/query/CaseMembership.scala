package org.cafienne.service.db.query

import org.cafienne.actormodel.identity.{CaseUserIdentity, Origin}

class CaseMembership(override val id: String, override val origin: Origin, override val tenantRoles: Set[String], val caseInstanceId: String, val tenant: String) extends CaseUserIdentity