/*
 * Copyright (C) 2019-2023 Batav B.V. <https://batav.io/tothepoint/license>
 */

package org.cafienne.querydb.query.board

import scala.concurrent.Future

class BoardQueriesImpl extends BoardQueries {
  override def getBoard(boardId: String): Future[Option[BoardQueryProtocol.Board]] = ???

  override def getBoards(userContext: String): Future[Seq[BoardQueryProtocol.Board]] = ???

  override def getColumnsByBoards(ids: Seq[String]): Future[Seq[BoardQueryProtocol.Column]] = ???

  override def getTasksByBoards(ids: Seq[String]): Future[Seq[BoardQueryProtocol.Task]] = ???

  override def getTask(taskId: String): Future[Option[BoardQueryProtocol.Task]] = ???

  override def getRelatedFlowTasks(taskId: String): Future[Seq[String]] = ???

  override def getFlowByTask(taskId: String): Future[Option[BoardQueryProtocol.Flow]] = ???

  override def getFlows(): Future[Seq[BoardQueryProtocol.Flow]] = ???

  override def getFlow(flowId: String): Future[Option[BoardQueryProtocol.Flow]] = ???

  override def getTeamByBoards(ids: Seq[String]): Future[Seq[BoardQueryProtocol.TeamMember]] = ???

  override def getRolesByBoards(ids: Seq[String]): Future[Seq[String]] = ???

  def getTeamMembershipByUser(userId: String): Future[Seq[BoardQueryProtocol.TeamMember]] = ???

}
