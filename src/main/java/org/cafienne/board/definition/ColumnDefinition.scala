package org.cafienne.board.definition

import org.cafienne.board.actorapi.event.definition.{ColumnDefinitionAdded, ColumnDefinitionUpdated}
import org.cafienne.json.ValueMap

class ColumnDefinition(val columnId: String, val board: BoardDefinition, val previous: Option[ColumnDefinition]) {
  val position = board.columns.size
  board.columns += this // This implicitly increases count of next column

  private val columnIdentifier = s"${board.boardId}_${position}"
  private val sentryIdentifier = previous.fold("")(_ => s"crit__${columnIdentifier}")
  private val taskIdentifier = s"ht__${columnIdentifier}"

  lazy val entryCriterion: String = previous.fold("")(_ => {
    s"""<entryCriterion id="_entry_${board.boardId}_${position}" name="EntryCriterion_${position}" sentryRef="${sentryIdentifier}"/>"""
  })
  lazy val sentry: String = previous.fold("")(column => {
    s"""<sentry id="${sentryIdentifier}">
       |   <planItemOnPart id="_${board.boardId}_7" sourceRef="pi_ht__${column.columnIdentifier}">
       |      <standardEvent>complete</standardEvent>
       |   </planItemOnPart>
       |</sentry>""".stripMargin
  })

  private var title: String = ""
  private var form: ValueMap = new ValueMap()

  def updateState(event: ColumnDefinitionAdded): Unit = {
    this.title = event.title
    this.form = event.form.getOrElse(this.form)
  }

  def updateState(event: ColumnDefinitionUpdated): Unit = {
    this.title = event.title.getOrElse(this.title)
    this.form = event.form.getOrElse(this.form)
  }

  def planItemXML: String = {
    s"""<planItem id="pi_ht__${columnIdentifier}" name="${title}" definitionRef="${taskIdentifier}">
       |   ${entryCriterion}
       |      </planItem>
       |${sentry}""".stripMargin
  }

  def humanTaskXML: String = {
    val metadata = s"${columnIdentifier}_BoardMetadata"
    val data = s"${columnIdentifier}_Data"
    val inMetadata = s"_ht_in${metadata}"
    val inData = s"_ht_in${data}"
    val outMetadata = s"_ht_out${metadata}"
    val outData = s"_ht_out${data}"

    s"""<humanTask id="${taskIdentifier}" name="${title}" isBlocking="true">
       |    <inputs id="${inMetadata}" name="BoardMetadata" bindingRef="${board.boardFileIdentifier}"/>
       |    <inputs id="${inData}" name="Data" bindingRef="${board.dataFileIdentifier}"/>
       |    <outputs id="${outMetadata}" name="BoardMetadata" bindingRef="${board.boardFileIdentifier}"/>
       |    <outputs id="${outData}" name="Data" bindingRef="${board.dataFileIdentifier}"/>
       |    <extensionElements mustUnderstand="false">
       |        <cafienne:implementation name="myboard_col2" xmlns:cafienne="org.cafienne" class="org.cafienne.cmmn.definition.task.WorkflowTaskDefinition" humanTaskRef="myboard_col2.humantask">
       |            <input id="in_${columnIdentifier}_BoardMetadata" name="BoardMetadata"/>
       |            <input id="in_${columnIdentifier}_Data" name="Data"/>
       |            <output id="out_${columnIdentifier}_BoardMetadata" name="BoardMetadata"/>
       |            <output id="out_${columnIdentifier}_Data" name="Data"/>
       |            <parameterMapping id="_${board.boardId}_12" sourceRef="${inMetadata}" targetRef="in_${columnIdentifier}_BoardMetadata"/>
       |            <parameterMapping id="_${board.boardId}_13" sourceRef="${inData}" targetRef="in_${columnIdentifier}_Data"/>
       |            <parameterMapping id="_${board.boardId}_14" sourceRef="out_${columnIdentifier}_BoardMetadata" targetRef="${outMetadata}"/>
       |            <parameterMapping id="_${board.boardId}_15" sourceRef="out_${columnIdentifier}_Data" targetRef="${outData}"/>
       |            <task-model>
       |                ${form.toString}
       |            </task-model>
       |        </cafienne:implementation>
       |    </extensionElements>
       |</humanTask>""".stripMargin
  }
}
