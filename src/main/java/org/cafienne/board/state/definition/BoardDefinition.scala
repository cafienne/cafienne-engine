package org.cafienne.board.state.definition

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.event.BoardCreated
import org.cafienne.board.actorapi.event.definition.{BoardDefinitionEvent, BoardDefinitionUpdated, ColumnDefinitionAdded, ColumnDefinitionUpdated}
import org.cafienne.board.actorapi.event.team.BoardTeamEvent
import org.cafienne.board.state.StateElement
import org.cafienne.cmmn.definition.{CaseDefinition, DefinitionsDocument}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueList, ValueMap}
import org.cafienne.util.XMLHelper
import org.w3c.dom.Document

import scala.collection.mutable.ListBuffer

/**
  *
  * @param boardId - Resembles in the case definition
  */
class BoardDefinition(val board: BoardActor, val boardId: String, val optionalTitle: Option[String] = None) extends StateElement with CafienneJson with LazyLogging {
  /**
    * Identifier of case file item holding board metadata such as title.
    */
  val boardFileIdentifier = s"_${boardId}_BoardMetadata"
  /**
    * Identifier of case file item holding board free format data.
    */
  val dataFileIdentifier = s"_${boardId}_Data"

  val team: TeamDefinition = new TeamDefinition(this)

  /**
    * List of columns defined in the board
    */
  val columns: ListBuffer[ColumnDefinition] = ListBuffer()

  private var title: String = optionalTitle.getOrElse("")

  private var startForm: ValueMap = new ValueMap()

  def getTitle: String = title

  def getStartForm: ValueMap = startForm

  def recoveryCompleted(): Unit = team.recoveryCompleted()

  def updateState(event: BoardDefinitionEvent): Unit = event match {
    case event: BoardCreated =>
      title = event.title
      team.updateState(event)
    case event: BoardDefinitionUpdated =>
      this.title = event.title.getOrElse(this.title)
      this.startForm = event.form.getOrElse(this.startForm)
    case event: ColumnDefinitionAdded => new ColumnDefinition(this, event.columnId).updateState(event)
    case event: ColumnDefinitionUpdated => columns.find(_.columnId == event.columnId).foreach(_.updateState(event)) // Note: ignores event when column not found (which would be kinda really weird anyways)
    case event: BoardTeamEvent => team.updateState(event)
    case other => logger.warn(s"Board Definition cannot handle event of type ${other.getClass.getName}")
  }

  def caseDefinition: CaseDefinition = {
    val string =
      s"""<definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL" xmlns:cafienne="org.cafienne">
         |    <caseFileItemDefinition name="ttpboard" definitionType="http://www.omg.org/spec/CMMN/DefinitionType/Unspecified" id="ttpboard.cfid">
         |        <property name="${BoardDefinition.BOARD_IDENTIFIER}" type="http://www.omg.org/spec/CMMN/PropertyType/string">
         |            <extensionElements mustUnderstand="false">
         |                <cafienne:implementation xmlns:cafienne="org.cafienne" isBusinessIdentifier="true"/>
         |            </extensionElements>
         |        </property>
         |        <property name="subject" type="http://www.omg.org/spec/CMMN/PropertyType/string">
         |            <extensionElements mustUnderstand="false">
         |                <cafienne:implementation xmlns:cafienne="org.cafienne" isBusinessIdentifier="true"/>
         |            </extensionElements>
         |        </property>
         |         |    </caseFileItemDefinition>
         |    <caseFileItemDefinition name="ttpdata" definitionType="http://www.omg.org/spec/CMMN/DefinitionType/Unspecified" id="ttpdata.cfid"/>
         |    <case id="${title}.case" name="${title}" expressionLanguage="spel">
         |        <caseFileModel>
         |            <caseFileItem id="${boardFileIdentifier}" name="BoardMetadata" multiplicity="ExactlyOne" definitionRef="ttpboard.cfid"/>
         |            <caseFileItem id="${dataFileIdentifier}" name="Data" multiplicity="ExactlyOne" definitionRef="ttpdata.cfid"/>
         |        </caseFileModel>
         |        <casePlanModel id="cm__${boardId}_0" name="${title}" autoComplete="true">
         |            ${columns.map(_.planItemXML).mkString("\n")}
         |            ${columns.map(_.humanTaskXML).mkString("\n")}
         |
         |        </casePlanModel>
         |        ${team.caseTeamXML()}
         |        <input id="_in_case_${boardFileIdentifier}" name="BoardMetadata" bindingRef="${boardFileIdentifier}"/>
         |        <input id="_in_case_${dataFileIdentifier}" name="Data" bindingRef="${dataFileIdentifier}"/>
         |        <extensionElements mustUnderstand="false">
         |          <cafienne:start-case-model xmlns:cafienne="org.cafienne">
         |            <![CDATA[${startForm.toString}]]>
         |          </cafienne:start-case-model>
         |        </extensionElements>
         |    </case>
         |</definitions>""".stripMargin
    val xml: Document = XMLHelper.loadXML(string)
    val definitions = new DefinitionsDocument(xml)
    definitions.getFirstCase
  }

  override def toValue: Value[_] = {
    new ValueMap(Fields.id, boardId, Fields.title, title, Fields.team, team.toValue, Fields.columns, new ValueList(columns.map(_.toValue).toArray))
  }
}

object BoardDefinition {
  val BOARD_IDENTIFIER = "__ttp__boardId__"

  def deserialize(json: ValueMap): BoardDefinition = {
    val definition = new BoardDefinition(null, json.readString(Fields.id), json.readOption[String](Fields.title))
    definition.team.users.addAll(TeamDefinition.deserialize(json.withArray(Fields.team)))
    json.withArray(Fields.columns).getValue.toArray(Array[ValueMap]()).map(ColumnDefinition.deserialize(definition, _))
    definition
  }
}
