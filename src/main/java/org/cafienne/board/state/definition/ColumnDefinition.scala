package org.cafienne.board.state.definition

import org.cafienne.board.actorapi.event.definition.{ColumnDefinitionRemoved, WriteColumnDefinitionEvent}
import org.cafienne.board.state.BoardState
import org.cafienne.infrastructure.serialization.Fields
import org.cafienne.json.{CafienneJson, Value, ValueMap}

class ColumnDefinition(val state: BoardState, val columnId: String) extends DefinitionElement with FormElement with CafienneJson {
  def previous: Option[ColumnDefinition] = {
    if (this.position == 0) {
      None
    } else {
      Some(definition.columns(this.position - 1))
    }
  }

  def next: Option[ColumnDefinition] = {
    println(s"Getting next for position ${position}")
    val n = if (this.position == definition.columns.size - 1) {
      None
    } else {
      Some(definition.columns(this.position + 1))
    }
    println(s"Found n: ${n}")
    n
  }

  def position: Int = definition.columns.indexOf(this)
  definition.columns += this // This implicitly increases count of next column

  private def columnIdentifier = s"${definition.boardId}_$position"
  private def enterStageSentryIdentifier = s"crit__enter_$columnIdentifier"
  private def exitStageSentryIdentifier = previous.fold("")(_ => s"crit__exit_$columnIdentifier")
  private def returnToStageSentryIdentifier = next.fold("")(_ => s"crit__return_$columnIdentifier")
  private def taskName = s"$title"
  private def taskIdentifier = s"ht__$columnIdentifier"
  private def taskPlanItemIdentifier: String = s"pi_$taskIdentifier"
  private def eventName = s"cancel_$taskName"
  private def eventIdentifier: String = s"ue__cancel_$columnIdentifier"
  private def eventPlanItemIdentifier: String = s"pi_$eventIdentifier"
  private def stageName = s"stage_$taskName"
  private def stageIdentifier = s"st__$columnIdentifier"
  private def stagePlanItemIdentifier = s"pi_$stageIdentifier"

  def updateState(event: WriteColumnDefinitionEvent): Unit = {
    this.title = event.title
    this.role = event.role
    this.form = event.form
  }

  def updateState(event: ColumnDefinitionRemoved): Unit = {
    definition.columns.remove(position, 1)
  }

  def columnPlanItemDefinitionXML: String =
    s"""$humanTaskXML
       |$eventXML
       |$stageXML
       |""".stripMargin

  def columnPlanItemXML: String = s"""$stagePlanItemXML""".stripMargin

  private def humanTaskPlanItemXML: String = s"""<planItem id="$taskPlanItemIdentifier" name="$taskName" definitionRef="$taskIdentifier"></planItem>"""

  private def enterStageSentryXML: String = {
    // Either listen to the human task in the previous column completes, or to the case to start, which is when the case file metadata item is created
    val onPart = previous.fold(
      s"""    <caseFileItemOnPart id="_on_create__${definition.boardFileIdentifier}" sourceRef="${definition.boardFileIdentifier}">
         |        <standardEvent>create</standardEvent>
         |    </caseFileItemOnPart>
         """.stripMargin)(previous =>
      s"""    <planItemOnPart id="_on_complete__${previous.taskPlanItemIdentifier}" sourceRef="${previous.taskPlanItemIdentifier}">
         |        <standardEvent>complete</standardEvent>
         |    </planItemOnPart>
         """.stripMargin)

    s"""<sentry id="$enterStageSentryIdentifier">
       |$onPart
       |</sentry>
       |""".stripMargin
  }

  private def exitStageSentryXML: String = previous.fold("")(_ => {
    s"""<sentry id="$exitStageSentryIdentifier">
       |    <planItemOnPart id="_on_occur__$eventPlanItemIdentifier" sourceRef="$eventPlanItemIdentifier">
       |        <standardEvent>occur</standardEvent>
       |    </planItemOnPart>
       |</sentry>
       |""".stripMargin
  })

  private def returnToStageSentryXML: String = next.fold("")(next => {
    s"""<sentry id="$returnToStageSentryIdentifier">
       |    <planItemOnPart id="_on_occur__${next.eventPlanItemIdentifier}" sourceRef="${next.eventPlanItemIdentifier}">
       |        <standardEvent>occur</standardEvent>
       |    </planItemOnPart>
       |</sentry>
       |""".stripMargin
  })

  def columnSentryXML: String =
    s"""$enterStageSentryXML
       |$exitStageSentryXML
       |$returnToStageSentryXML
       |""".stripMargin

  def humanTaskXML: String = {
    val metadata = s"${columnIdentifier}_BoardMetadata"
    val data = s"${columnIdentifier}_Data"
    val inFileMetadata = s"_ht_in$metadata"
    val inFileData = s"_ht_in$data"
    val outFileMetadata = s"_ht_out$metadata"
    val outFileData = s"_ht_out$data"
    val taskImplementation = s"${definition.getTitle}_${this.getTitle}"
    val inTaskMetadata = s"in_${columnIdentifier}_BoardMetadata"
    val inTaskData = s"in_${columnIdentifier}_Data"
    val outTaskMetadata = s"out_${columnIdentifier}_BoardMetadata"
    val outTaskData = s"out_${columnIdentifier}_Data"

    s"""<humanTask id="$taskIdentifier" name="$taskName" isBlocking="true" performerRef="$role">
       |    <inputs id="$inFileMetadata" name="BoardMetadata" bindingRef="${definition.boardFileIdentifier}"/>
       |    <inputs id="$inFileData" name="Data" bindingRef="${definition.dataFileIdentifier}"/>
       |    <outputs id="$outFileMetadata" name="BoardMetadata" bindingRef="${definition.boardFileIdentifier}"/>
       |    <outputs id="$outFileData" name="Data" bindingRef="${definition.dataFileIdentifier}"/>
       |    <extensionElements mustUnderstand="false">
       |        <cafienne:implementation name="$taskImplementation" xmlns:cafienne="org.cafienne" class="org.cafienne.cmmn.definition.task.WorkflowTaskDefinition" humanTaskRef="$taskImplementation.humantask">
       |            <input id="$inTaskMetadata" name="BoardMetadata"/>
       |            <input id="$inTaskData" name="Data"/>
       |            <output id="$outTaskMetadata" name="BoardMetadata"/>
       |            <output id="$outTaskData" name="Data"/>
       |            <parameterMapping id="_${definition.boardId}_12" sourceRef="$inFileMetadata" targetRef="$inTaskMetadata"/>
       |            <parameterMapping id="_${definition.boardId}_13" sourceRef="$inFileData" targetRef="$inTaskData"/>
       |            <parameterMapping id="_${definition.boardId}_14" sourceRef="$outTaskMetadata" targetRef="$outFileMetadata"/>
       |            <parameterMapping id="_${definition.boardId}_15" sourceRef="$outTaskData" targetRef="$outFileData"/>
       |            <task-model>
       |                ${form.toString}
       |            </task-model>
       |        </cafienne:implementation>
       |    </extensionElements>
       |</humanTask>
       |""".stripMargin
  }

  def eventXML: String = s"""<userEvent id="$eventIdentifier" name="$eventName"/>""".stripMargin

  def eventPlanItemXML: String = s"""<planItem id="$eventPlanItemIdentifier" name="$eventName" definitionRef="$eventIdentifier"/>"""

  def stageXML: String = {
    s"""<stage id="$stageIdentifier" name="$stageName" autoComplete="true">
       |    $humanTaskPlanItemXML
       |    $eventPlanItemXML
       |</stage>
       |""".stripMargin
  }

  private def enterStageCriterionXML: String = s"""<entryCriterion id="_enter__$taskName" name="EntryCriterion_$position" sentryRef="$enterStageSentryIdentifier"/>"""

  private def exitStageCriterionXML: String = previous.fold("")(_ => s"""<exitCriterion id="_exit__$taskName" name="ExitCriterion_$position" sentryRef="$exitStageSentryIdentifier"/>""")

  private def returnToStageCriterionXML: String = next.fold("")(_ => s"""<entryCriterion id="_entry_${definition.boardId}_$position" name="ReturnToCriterion_$position" sentryRef="$returnToStageSentryIdentifier"/>""")

  def stagePlanItemXML: String =
    s"""<planItem id="$stagePlanItemIdentifier" name="$stageName" definitionRef="$stageIdentifier">
       |    $enterStageCriterionXML
       |    $exitStageCriterionXML
       |    $returnToStageCriterionXML
       |    <itemControl id="ic_$stagePlanItemIdentifier">
       |        <repetitionRule id="rr_$stagePlanItemIdentifier">
       |            <condition id="c_$stagePlanItemIdentifier">
       |                <body>
       |                    <![CDATA[true]]>
       |                 </body>
       |            </condition>
       |        </repetitionRule>
       |    </itemControl>
       |</planItem>
       |""".stripMargin


  override def toValue: Value[_] = new ValueMap(Fields.columnId, columnId, Fields.position, position, Fields.title, title, Fields.role, role, Fields.form, form)
}

object ColumnDefinition {
  def deserialize(definition: BoardDefinition, json: ValueMap): ColumnDefinition = {
    val id = json.readString(Fields.columnId)
    val column = new ColumnDefinition(definition.state, id)
    column.title = json.readString(Fields.title)
    column.role = json.readString(Fields.role)
    column.form = json.readMap(Fields.form)
    column
  }
}
