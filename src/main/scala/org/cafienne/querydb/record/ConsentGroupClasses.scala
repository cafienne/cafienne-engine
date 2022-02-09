package org.cafienne.querydb.record

final case class ConsentGroupRecord(id: String, tenant: String)

final case class ConsentGroupMemberRecord(group: String, userId: String, role: String = "", isOwner: Boolean = false)
