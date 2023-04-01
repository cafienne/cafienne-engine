/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.querydb.query.exception

class SearchFailure(msg: String) extends RuntimeException(msg)

case class CaseSearchFailure(caseId: String) extends SearchFailure(s"A case with id '$caseId' cannot be found.")
case class TaskSearchFailure(taskId: String) extends SearchFailure(s"A task with id '$taskId' cannot be found.")
case class PlanItemSearchFailure(planItemId: String) extends SearchFailure(s"A plan item with id '$planItemId' cannot be found.")
case class UserSearchFailure(userId: String) extends SearchFailure(s"A user with id '$userId' cannot be found.")
case class TenantSearchFailure(tenantId: String) extends SearchFailure(s"An active tenant with id '$tenantId' cannot be found.")
case class TenantUserSearchFailure(tenantId: String, user: String) extends SearchFailure(s"Tenant '$tenantId' does not exist, or user '$user' is not registered in it")
case class ConsentGroupSearchFailure(groupId: String) extends SearchFailure(s"A consent group with id '$groupId' cannot be found.")
case class ConsentGroupMemberSearchFailure(userId: String) extends SearchFailure(s"A consent group member with user id '$userId' cannot be found.")
case class BoardSearchFailure(boardId: String) extends SearchFailure(s"A board with id '$boardId' cannot be found.")
