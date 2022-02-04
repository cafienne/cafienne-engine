package org.cafienne.service.db.query.exception

class SearchFailure(msg: String) extends RuntimeException(msg)

case class CaseSearchFailure(caseId: String) extends SearchFailure(s"A case with id '$caseId' cannot be found.")
case class TaskSearchFailure(taskId: String) extends SearchFailure(s"A task with id '$taskId' cannot be found.")
case class PlanItemSearchFailure(planItemId: String) extends SearchFailure(s"A plan item with id '$planItemId' cannot be found.")
case class UserSearchFailure(userId: String) extends SearchFailure(s"A user with id '$userId' cannot be found.")
case class TenantSearchFailure(tenantId: String) extends SearchFailure(s"An active tenant with id '$tenantId' cannot be found.")
case class ConsentGroupSearchFailure(groupId: String) extends SearchFailure(s"A consent group with id '$groupId' cannot be found.")
case class ConsentGroupMemberSearchFailure(userId: String) extends SearchFailure(s"A consent group member with user id '$userId' cannot be found.")
