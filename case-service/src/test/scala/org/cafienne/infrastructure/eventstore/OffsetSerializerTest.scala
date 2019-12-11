package org.cafienne.infrastructure.eventstore

import org.cafienne.infrastructure.eventstore.OffsetType.{NoOffsetType, SequenceType, TimeBasedUUIDType}
import org.scalatest.{FlatSpec, Matchers}

class OffsetSerializerTest extends FlatSpec with Matchers {

  val offsetSerializer = new OffsetSerializer

  "serialize and deserialize" should "get the same object for noOffset" in {
    val wo = WrappedOffset(NoOffsetType)

    val serb = offsetSerializer.toBinary(wo)
    val wo2 = offsetSerializer.fromBinary(serb)

    wo2 should be(wo)
  }

  it should "get the same object for TimebasedOffset" in {
    val wo = WrappedOffset(TimeBasedUUIDType, java.util.UUID.randomUUID().toString)

    val serb = offsetSerializer.toBinary(wo)
    val wo2 = offsetSerializer.fromBinary(serb)

    wo2 should be(wo)
  }

  it should "get the same object for Sequence" in {
    val wo = WrappedOffset(SequenceType, 345L.toString)

    val serb = offsetSerializer.toBinary(wo)
    val wo2 = offsetSerializer.fromBinary(serb)

    wo2 should be(wo)
  }

}
