package org.cafienne.board.state.definition

import org.cafienne.board.actorapi.event.definition.{ColumnDefinitionRemoved, WriteColumnDefinitionEvent}
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

class ColumnDefinition(val definition: BoardDefinition, val columnId: String) extends DefinitionElement with CafienneJson {
  def previous: Option[ColumnDefinition] = {
    if (this.position == 0) {
      None
    } else {
      Some(definition.columns(this.position - 1))
    }
  }
  def position: Int = definition.columns.indexOf(this)
  definition.columns += this // This implicitly increases count of next column

  private def columnIdentifier = s"${definition.boardId}_${position}"
  private def sentryIdentifier = previous.fold("")(_ => s"crit__${columnIdentifier}")
  private def taskIdentifier = s"ht__${columnIdentifier}"

  lazy val entryCriterion: String = previous.fold("")(_ => {
    s"""<entryCriterion id="_entry_${definition.boardId}_${position}" name="EntryCriterion_${position}" sentryRef="${sentryIdentifier}"/>"""
  })
  lazy val sentry: String = previous.fold("")(column => {
    s"""<sentry id="${sentryIdentifier}">
       |   <planItemOnPart id="_${definition.boardId}_7" sourceRef="pi_ht__${column.columnIdentifier}">
       |      <standardEvent>complete</standardEvent>
       |   </planItemOnPart>
       |</sentry>""".stripMargin
  })

  private var title: String = ""
  private var role: String = ""
  private var form: ValueMap = new ValueMap()

  def getTitle: String = title

  def getRole: String = role

  def getForm: ValueMap = form

  def updateState(event: WriteColumnDefinitionEvent): Unit = {
    this.title = event.title.getOrElse(this.title)
    this.role = event.role.getOrElse(this.role)
    this.form = event.form.getOrElse(this.form)
  }

  def updateState(event: ColumnDefinitionRemoved): Unit = {
    definition.columns.remove(position, 1)
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

    s"""<humanTask id="$taskIdentifier" name="$title" isBlocking="true" performerRef="$role">
       |    <inputs id="$inMetadata" name="BoardMetadata" bindingRef="${definition.boardFileIdentifier}"/>
       |    <inputs id="$inData" name="Data" bindingRef="${definition.dataFileIdentifier}"/>
       |    <outputs id="$outMetadata" name="BoardMetadata" bindingRef="${definition.boardFileIdentifier}"/>
       |    <outputs id="$outData" name="Data" bindingRef="${definition.dataFileIdentifier}"/>
       |    <extensionElements mustUnderstand="false">
       |        <cafienne:implementation name="myboard_col2" xmlns:cafienne="org.cafienne" class="org.cafienne.cmmn.definition.task.WorkflowTaskDefinition" humanTaskRef="myboard_col2.humantask">
       |            <input id="in_${columnIdentifier}_BoardMetadata" name="BoardMetadata"/>
       |            <input id="in_${columnIdentifier}_Data" name="Data"/>
       |            <output id="out_${columnIdentifier}_BoardMetadata" name="BoardMetadata"/>
       |            <output id="out_${columnIdentifier}_Data" name="Data"/>
       |            <parameterMapping id="_${definition.boardId}_12" sourceRef="$inMetadata" targetRef="in_${columnIdentifier}_BoardMetadata"/>
       |            <parameterMapping id="_${definition.boardId}_13" sourceRef="$inData" targetRef="in_${columnIdentifier}_Data"/>
       |            <parameterMapping id="_${definition.boardId}_14" sourceRef="out_${columnIdentifier}_BoardMetadata" targetRef="$outMetadata"/>
       |            <parameterMapping id="_${definition.boardId}_15" sourceRef="out_${columnIdentifier}_Data" targetRef="$outData"/>
       |            <task-model>
       |                ${form.toString}
       |            </task-model>
       |        </cafienne:implementation>
       |    </extensionElements>
       |</humanTask>""".stripMargin
  }

  override def toValue: Value[_] = new ValueMap(Fields.columnId, columnId, Fields.position, position, Fields.title, title, Fields.role, role, Fields.form, form)
}

object ColumnDefinition {
  def deserialize(definition: BoardDefinition, json: ValueMap): ColumnDefinition = {
    val id = json.readString(Fields.columnId)
    val column = new ColumnDefinition(definition, id)
    column.title = json.readString(Fields.title)
    column.role = json.readString(Fields.role)
    column.form = json.readMap(Fields.form)
    column
  }
}
