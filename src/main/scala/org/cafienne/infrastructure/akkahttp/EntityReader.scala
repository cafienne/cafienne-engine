package org.cafienne.infrastructure.akkahttp

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.unmarshalling.Unmarshaller
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.{ClassTagExtensions, DefaultScalaModule}
import org.cafienne.infrastructure.serialization.{ValueMapJacksonDeserializer, ValueMapJacksonSerializer}
import org.cafienne.json.ValueMap

/**
  * Helper classes to read akka http entity as an expected type
  */
object EntityReader {
  type EntityReader[T] = Unmarshaller[HttpEntity, T]

  private val mapper = new ObjectMapper() with ClassTagExtensions
  mapper.registerModule(DefaultScalaModule)

  val valueMapModule = new SimpleModule
  valueMapModule.addSerializer(classOf[ValueMap], new ValueMapJacksonSerializer)
  valueMapModule.addDeserializer(classOf[ValueMap], new ValueMapJacksonDeserializer)
  mapper.registerModule(valueMapModule)

  /**
    * Converts the incoming data to json and from there to a typed entity
    * @param m
    * @tparam T
    * @return
    */
  def entityReader[T](implicit m : Manifest[T]): Unmarshaller[HttpEntity, T] = Unmarshaller.stringUnmarshaller.forContentTypes(ContentTypes.`application/json`).map(data => mapper.readValue[T](data))
}
