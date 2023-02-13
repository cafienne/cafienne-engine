package org.cafienne.board.definition

import org.cafienne.cmmn.definition.{CaseDefinition, DefinitionsDocument}
import org.cafienne.util.{Guid, XMLHelper}
import org.w3c.dom.Document

import scala.collection.mutable.ListBuffer

class BoardDefinition(val title: String) {

  /**
    * Used inside case definition to have unique element names
    */
  val guid = new Guid().toString
  /**
    * Identifier of case file item holding board metadata such as title.
    */
  val boardFileIdentifier = s"_${guid}_BoardMetadata"
  /**
    * Identifier of case file item holding board free format data.
    */
  val dataFileIdentifier = s"_${guid}_Data"

  /**
    * List of columns defined in the board
    */
  val columns: ListBuffer[ColumnDefinition] = ListBuffer()

  def startFlowForm = ""
  def caseDefinition: CaseDefinition = {
    val string =
      s"""<definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL" xmlns:cafienne="org.cafienne">
         |    <caseFileItemDefinition name="ttpboard" definitionType="http://www.omg.org/spec/CMMN/DefinitionType/Unspecified" id="ttpboard.cfid">
         |        <property name="title" type="http://www.omg.org/spec/CMMN/PropertyType/string">
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
         |        <casePlanModel id="cm__${guid}_0" name="${title}" autoComplete="true">
         |            ${columns.map(_.planItemXML).mkString("\n")}
         |            ${columns.map(_.humanTaskXML).mkString("\n")}
         |
         |        </casePlanModel>
         |        <input id="_in_case_${boardFileIdentifier}" name="BoardMetadata" bindingRef="${boardFileIdentifier}"/>
         |        <input id="_in_case_${dataFileIdentifier}" name="Data" bindingRef="${dataFileIdentifier}"/>
         |        <extensionElements mustUnderstand="false">
         |          <cafienne:start-case-model xmlns:cafienne="org.cafienne">
         |            ${startFlowForm}
         |          </cafienne:start-case-model>
         |        </extensionElements>
         |    </case>
         |</definitions>""".stripMargin
    val xml: Document = XMLHelper.loadXML(string)
    val definitions = new DefinitionsDocument(xml)
    definitions.getFirstCase
  }

  def addColumn(title: String): Unit = {
    new ColumnDefinition(title, this, columns.lastOption)
  }
}
