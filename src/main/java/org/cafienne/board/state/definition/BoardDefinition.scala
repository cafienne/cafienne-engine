package org.cafienne.board.state.definition

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.board.BoardActor
import org.cafienne.board.actorapi.event.BoardCreated
import org.cafienne.board.actorapi.event.definition.{BoardDefinitionEvent, BoardDefinitionUpdated, ColumnDefinitionAdded, ColumnDefinitionUpdated}
import org.cafienne.board.state.StateElement
import org.cafienne.cmmn.definition.{CaseDefinition, DefinitionsDocument}
import org.cafienne.json.ValueMap
import org.cafienne.util.XMLHelper
import org.w3c.dom.Document

import scala.collection.mutable.ListBuffer

/**
  *
  * @param boardId - Resembles in the case definition
  */
class BoardDefinition(val board: BoardActor) extends StateElement with LazyLogging {
  val boardId: String = board.getId
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

  private var title: String = ""

  private var startForm: ValueMap = new ValueMap()

  def getTitle: String = title

  def getStartForm: ValueMap = startForm

  def updateState(event: BoardDefinitionEvent) = event match {
    case event: BoardCreated => title = event.title
    case event: BoardDefinitionUpdated =>
      this.title = event.title.getOrElse(this.title)
      this.startForm = event.form.getOrElse(this.startForm)
    case event: ColumnDefinitionAdded => new ColumnDefinition(event.columnId, this, columns.lastOption).updateState(event)
    case event: ColumnDefinitionUpdated => {
      val column = columns.find(_.columnId == event.columnId).get
      column.updateState(event)
    }
    case other => logger.warn(s"Board Definition cannot handle event of type ${other.getClass.getName}")
  }

  def getCaseDefinition(): CaseDefinition = {
    val string =
      s"""<definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL" xmlns:cafienne="org.cafienne">
         |    <caseFileItemDefinition name="ttpboard" definitionType="http://www.omg.org/spec/CMMN/DefinitionType/Unspecified" id="ttpboard.cfid">
         |        <property name="subject" type="http://www.omg.org/spec/CMMN/PropertyType/string">
         |            <extensionElements mustUnderstand="false">
         |                <cafienne:implementation xmlns:cafienne="org.cafienne" isBusinessIdentifier="true"/>
         |            </extensionElements>
         |        </property>
         |    </caseFileItemDefinition>
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

  def addColumn(columnId: String, name: String, form: ValueMap): Unit = {
    new ColumnDefinition(columnId, this, columns.lastOption)
  }
}
