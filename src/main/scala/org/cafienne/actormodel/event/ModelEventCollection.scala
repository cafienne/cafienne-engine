package org.cafienne.actormodel.event

import scala.collection.mutable.ListBuffer

trait ModelEventCollection {
  val events: ListBuffer[ModelEvent] = ListBuffer()

  def eventsOfType[ME <: ModelEvent](clazz: Class[ME]): Seq[ME] = events.filter(event => clazz.isAssignableFrom(event.getClass)).map(_.asInstanceOf[ME]).toSeq

  def optionalEvent[ME <: ModelEvent](clazz: Class[ME]): Option[ME] = eventsOfType(clazz).headOption

  def getEvent[ME <: ModelEvent](clazz: Class[ME]): ME = optionalEvent(clazz).get
}
