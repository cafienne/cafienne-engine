package org.cafienne.board.state.definition

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.board.actorapi.event.BoardCreated
import org.cafienne.board.actorapi.event.definition._
import org.cafienne.board.state.BoardState
import org.cafienne.cmmn.definition.{CaseDefinition, DefinitionsDocument}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json._
import org.cafienne.util.XMLHelper
import org.w3c.dom.Document

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BoardDefinition(val state: BoardState) extends FormElement with CafienneJson with LazyLogging {
  /**
    * Identifier of case file item holding board metadata such as title.
    */
  val boardFileIdentifier = s"_${boardId}_BoardMetadata"
  /**
    * Identifier of case file item holding board free format data.
    */
  val dataFileIdentifier = s"_${boardId}_Data"

  val roles: mutable.Set[String] = new mutable.HashSet[String]()

  /**
    * List of columns defined in the board
    */
  val columns: ListBuffer[ColumnDefinition] = ListBuffer()

  def getStartForm: ValueMap = getForm

  def upsertTeamRole(roleName: String): Unit = {
    // Don't add blank roles
    if (!roleName.isBlank && !roles.contains(roleName)) board.addEvent(new RoleDefinitionAdded(this, roleName))
  }

  def updateState(event: BoardDefinitionEvent): Unit = event match {
    case event: BoardCreated =>
      title = event.title
      form = event.form
      board.state.team.updateState(event)
    case event: BoardDefinitionUpdated =>
      this.title = event.title
      this.form = event.form
    case event: ColumnDefinitionAdded => new ColumnDefinition(state, event.columnId).updateState(event)
    case event: ColumnDefinitionUpdated => columns.find(_.columnId == event.columnId).foreach(_.updateState(event)) // Note: ignores event when column not found (which would be kinda really weird anyways)
    case event: ColumnDefinitionRemoved => columns.find(_.columnId == event.columnId).foreach(_.updateState(event))
    case event: RoleDefinitionAdded => roles.add(event.roleName)
    case event: RoleDefinitionRemoved => roles.remove(event.roleName)
    case other => logger.warn(s"Board Definition cannot handle event of type ${other.getClass.getName}")
  }

  def removeColumn(column: ColumnDefinition): Unit = {
    columns.remove(column.position, 0)
  }

  def casePlanIdentifier: String = s"cm__${boardId}_0"

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
         |    </caseFileItemDefinition>
         |    <caseFileItemDefinition name="ttpdata" definitionType="http://www.omg.org/spec/CMMN/DefinitionType/Unspecified" id="ttpdata.cfid"/>
         |    <case id="$title.case" name="$title" expressionLanguage="spel">
         |        <caseFileModel>
         |            <caseFileItem id="$boardFileIdentifier" name="BoardMetadata" multiplicity="ExactlyOne" definitionRef="ttpboard.cfid"/>
         |            <caseFileItem id="$dataFileIdentifier" name="Data" multiplicity="ExactlyOne" definitionRef="ttpdata.cfid"/>
         |        </caseFileModel>
         |        <casePlanModel id="$casePlanIdentifier" name="$title" autoComplete="true">
         |            ${columns.map(_.columnPlanItemXML).mkString("\n")}
         |            ${columns.map(_.columnSentryXML).mkString("\n")}
         |            ${columns.map(_.columnPlanItemDefinitionXML).mkString("\n")}
         |        </casePlanModel>
         |        <caseRoles>
         |          ${roles.filterNot(_.isBlank).map(role => s"""<role id="${role}" name="${role}" />""").mkString("", "\n          ", "\n")}
         |        </caseRoles>
         |        <input id="_in_case_$boardFileIdentifier" name="BoardMetadata" bindingRef="$boardFileIdentifier"/>
         |        <input id="_in_case_$dataFileIdentifier" name="Data" bindingRef="$dataFileIdentifier"/>
         |        <extensionElements mustUnderstand="false">
         |          <cafienne:start-case-model xmlns:cafienne="org.cafienne">
         |            <![CDATA[${form.toString}]]>
         |          </cafienne:start-case-model>
         |        </extensionElements>
         |    </case>
         |    <CMMNDI>
         |        <CMMNDiagram>
         |            <CMMNShape cmmnElementRef="$casePlanIdentifier">
         |                <Bounds x="20" y="20" width="${320 * columns.size}" height="500"/>
         |            </CMMNShape>
         |            ${columns.map(_.shapesXML).mkString("\n")}
         |            ${columns.map(_.edgesXML).mkString("\n")}
         |        </CMMNDiagram>
         |    </CMMNDI>
         |</definitions>""".stripMargin
    val xml: Document = XMLHelper.loadXML(string)
    val definitions = new DefinitionsDocument(xml)
    definitions.getFirstCase
  }

  override def toValue: Value[_] = {
    new ValueMap(Fields.id, boardId, Fields.title, title, Fields.form, form, Fields.roles, roles, Fields.columns, new ValueList(columns.map(_.toValue).toArray))
  }
}

object BoardDefinition {
  val BOARD_IDENTIFIER = "__ttp__boardId__"

  def deserialize(state: BoardState, json: ValueMap): BoardDefinition = {
    val definition = new BoardDefinition(state)
    definition.title = json.readString(Fields.title)
    definition.form = json.readMap(Fields.form)
    json.withArray(Fields.columns).getValue.toArray(Array[ValueMap]()).map(ColumnDefinition.deserialize(definition, _))
    json.withArray(Fields.roles).getValue.toArray(Array[StringValue]()).map(_.getValue).foreach(definition.roles += _)
    definition
  }
}
