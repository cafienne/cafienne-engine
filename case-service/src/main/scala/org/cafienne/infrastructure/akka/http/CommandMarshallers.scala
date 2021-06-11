package org.cafienne.infrastructure.akka.http

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.cafienne.infrastructure.serialization.{ValueMapJacksonDeserializer, ValueMapJacksonSerializer}
import org.cafienne.json.ValueMap
import org.cafienne.infrastructure.serialization.ValueMapJacksonDeserializer
import org.cafienne.cmmn.actorapi.command.CaseCommandModels
import org.cafienne.service.api.anonymous.CaseRequestRoute.AnonymousStartCaseFormat
import org.cafienne.service.api.model.{BackwardCompatibleTeamFormat, BackwardCompatibleTeamMemberFormat, StartCaseFormat}

/**
  * This file contains some marshallers and unmarshallers for the engine
  */
object CommandMarshallers {

  implicit val StartCaseUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[StartCaseFormat](data)
  })

  implicit val AnonymousStartCaseUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[AnonymousStartCaseFormat](data)
  })

  implicit val StartCaseMarshaller = Marshaller.withFixedContentType(ContentTypes.`application/json`) { value: StartCaseFormat =>
    val startCaseJson = JsonUtil.toJson(value)
    HttpEntity(ContentTypes.`application/json`, startCaseJson)
  }

  implicit val CaseTeamUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[BackwardCompatibleTeamFormat](data)
  })

  implicit val CaseTeamMemberUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[BackwardCompatibleTeamMemberFormat](data)
  })

  implicit val DiscretionaryItemUnMarshaller = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => {
    JsonUtil.fromJson[CaseCommandModels.PlanDiscretionaryItem](data)
  })
}

object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  val valueMapModule = new SimpleModule
  valueMapModule.addSerializer(classOf[ValueMap], new ValueMapJacksonSerializer)
  valueMapModule.addDeserializer(classOf[ValueMap], new ValueMapJacksonDeserializer)
  mapper.registerModule(valueMapModule)

  def toJson(value: Any): String = {
    mapper.writeValueAsString(value)
  }

  def fromJson[T](json: String)(implicit m : Manifest[T]): T = {
    mapper.readValue[T](json)
  }
}