package org.cafienne.querydb.query

import org.cafienne.actormodel.identity.{CaseUserIdentity, ConsentGroupMembership, Origin}

class CaseMembership(override val id: String, override val origin: Origin, override val tenantRoles: Set[String], override val groups: Seq[ConsentGroupMembership], val caseInstanceId: String, val tenant: String) extends CaseUserIdentity