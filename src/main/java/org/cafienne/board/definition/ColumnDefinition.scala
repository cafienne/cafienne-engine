package org.cafienne.board.definition

class ColumnDefinition(val name: String = "Column", val board: BoardDefinition, val previous: Option[ColumnDefinition] = None) {
  val position = board.columns.size
  board.columns += this // This implicitly increases count of next column

  private val columnIdentifier = s"${board.guid}_${position}"
  private val sentryIdentifier = previous.fold("")(_ => s"crit__${columnIdentifier}")
  private val taskIdentifier = s"ht__${columnIdentifier}"

  lazy val entryCriterion: String = previous.fold("")(_ => {
    s"""<entryCriterion id="_entry_${board.guid}_${position}" name="EntryCriterion_${position}" sentryRef="${sentryIdentifier}"/>"""
  })
  lazy val sentry: String = previous.fold("")(column => {
    s"""<sentry id="${sentryIdentifier}">
       |   <planItemOnPart id="_${board.guid}_7" sourceRef="pi_ht__${column.columnIdentifier}">
       |      <standardEvent>complete</standardEvent>
       |   </planItemOnPart>
       |</sentry>""".stripMargin
  })

  def planItemXML: String = {
    s"""<planItem id="pi_ht__${columnIdentifier}" name="${name}" definitionRef="${taskIdentifier}">
       |   ${entryCriterion}
       |      </planItem>
       |${sentry}""".stripMargin
  }

  lazy val form: String = ""

  def humanTaskXML: String = {
    val metadata = s"${columnIdentifier}_BoardMetadata"
    val data = s"${columnIdentifier}_Data"
    val inMetadata = s"_ht_in${metadata}"
    val inData = s"_ht_in${data}"
    val outMetadata = s"_ht_out${metadata}"
    val outData = s"_ht_out${data}"

    s"""<humanTask id="${taskIdentifier}" name="${name}" isBlocking="true">
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
       |            <parameterMapping id="_${board.guid}_12" sourceRef="${inMetadata}" targetRef="in_${columnIdentifier}_BoardMetadata"/>
       |            <parameterMapping id="_${board.guid}_13" sourceRef="${inData}" targetRef="in_${columnIdentifier}_Data"/>
       |            <parameterMapping id="_${board.guid}_14" sourceRef="out_${columnIdentifier}_BoardMetadata" targetRef="${outMetadata}"/>
       |            <parameterMapping id="_${board.guid}_15" sourceRef="out_${columnIdentifier}_Data" targetRef="${outData}"/>
       |            <task-model>
       |                ${form}
       |            </task-model>
       |        </cafienne:implementation>
       |    </extensionElements>
       |</humanTask>""".stripMargin
  }
}
