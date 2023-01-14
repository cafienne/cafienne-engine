/*
 * Copyright (C) 2019-2023 Batav B.V. <https://batav.io/tothepoint/license>
 */

package org.cafienne.querydb.query.board

import scala.concurrent.Future

trait BoardQueries {
  import BoardQueryProtocol._

  def getBoard(boardId: String): Future[Option[Board]]

  //TODO String parameter is the current user
  def getBoards(userContext: String): Future[Seq[Board]]
  def getColumnsByBoards(ids: Seq[String]): Future[Seq[Column]]

  //TODO String parameter is a list of boardIds
  def getTasksByBoards(ids: Seq[String]): Future[Seq[Task]]
  def getTask(taskId: String): Future[Option[Task]]
  def getRelatedFlowTasks(taskId: String): Future[Seq[String]]
  def getFlowByTask(taskId: String): Future[Option[Flow]]
  def getFlow(flowId: String): Future[Option[Flow]]
  def getFlows(): Future[Seq[Flow]]
  def getTeamByBoards(ids: Seq[String]): Future[Seq[TeamMember]]
  def getRolesByBoards(ids: Seq[String]): Future[Seq[String]]
  def getTeamMembershipByUser(userId: String): Future[Seq[TeamMember]]
}
