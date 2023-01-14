/*
 * Copyright (C) 2019-2023 Batav B.V. <https://batav.io/tothepoint/license>
 */

package org.cafienne.querydb.query.board

import io.swagger.v3.oas.annotations.media.Schema

import scala.annotation.meta.field

//TODO needs (de)serialization
object BoardQueryProtocol {

  case class BoardColumnKey(boardId: String, columnId: String)

  // "A board used to coordinate work"
  final case class Board(
    @(Schema @field)(implementation = classOf[String], example = "User id (matched with token when user logs on)")
    id: String,
    @(Schema @field)(implementation = classOf[String], example = "Title of the board.")
    title: String,
    @(Schema @field)(implementation = classOf[String], example = "The user ID that is the owner of the ttp.")
    ownerId: String
  )

  //"Team member that works with the ttp")
  final case class TeamMember(
    @(Schema @field)(implementation = classOf[String], example = "User ID of the team member")
    userId: String,
    //@GraphQLExclude
    boardId: String,
    @(Schema @field)(implementation = classOf[String], example = "Roles the user fulfills")
    roles: Seq[String],
    @(Schema @field)(implementation = classOf[String], example = "Name of the user")
    name: Option[String]
  ) {
    def initials() = name.fold(userId.charAt(0).toString + userId.last.toString)(n => n.split(" ").map(part => part.charAt(0)).mkString(""))
  }

  //("A column within a ttp.")
  final case class Column(
                           @(Schema @field)(implementation = classOf[String], example = "The unique jwtSub of the column within the ttp.")
    id: String,
                           @(Schema @field)(implementation = classOf[String], example = "Position of the column on the ttp (first left is position 0).")
    position: Int,
                           @(Schema @field)(implementation = classOf[String], example = "Title of the column.")
    title: Option[String],
    //@GraphQLExclude
    boardId: String,
                           @(Schema @field)(implementation = classOf[String], example = "Role allowed to execute tasks in this column, When empty everyone may execute tasks")
    role: Option[String]
  )

  /** A Flow is an instance of related tasks running through the columns
    * @param boardId
    * @param flowId
    * @param ownerId
    */
  final case class Flow(
    boardId: String,
    flowId: String,
    ownerId: String,
    subject: Option[String],
    description: Option[String],
    completed: Boolean
  )

  //"A task within a ttp.")
  final case class Task(
    @(Schema @field)(implementation = classOf[String], example = "The unique jwtSub of the task within the ttp.")
    id: String,
    @(Schema @field)(implementation = classOf[String], example = "The subject of the task")
    subject: Option[String],
    @(Schema @field)(implementation = classOf[String], example = "The description of the task")
    description: Option[String],
    @(Schema @field)(implementation = classOf[String], example = "Position of the task (in the column)")
    position: Int,
    //@GraphQLExclude
    boardId: String,
    //@GraphQLExclude
    columnId: String,
    @(Schema @field)(implementation = classOf[String], example = "Gives the jwtSub of the running flow this task belongs to")
    flowId: String,
    @(Schema @field)(implementation = classOf[String], example = "Identity of the one that claimed the task")
    claimedBy: Option[String]
  )

  //TODO replace with Cafienne serialization
//  implicit val boardViewItemFmt: RootJsonFormat[Board]           = jsonFormat3(Board)
//  implicit val teamFmt: RootJsonFormat[TeamMember]               = jsonFormat4(TeamMember)
//  implicit val columnViewItemFmt: RootJsonFormat[Column]         = jsonFormat5(Column)
//  implicit val flowsItemFmt: RootJsonFormat[Flow]                = jsonFormat6(Flow)
//  implicit val taskViewItemFmt: RootJsonFormat[Task]             = jsonFormat8(Task)
//  implicit val boardColumnKeyFmt: RootJsonFormat[BoardColumnKey] = jsonFormat2(BoardColumnKey)

}
