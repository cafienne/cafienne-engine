package org.cafienne.actormodel.identity

import org.cafienne.infrastructure.serialization.{DeserializationError, Fields}
import org.cafienne.json.{CafienneJson, Value, ValueMap}

trait UserIdentity extends CafienneJson {
  val id: String

  override def toValue: Value[_] = new ValueMap(Fields.userId, id)

  /**
    * Compatibility method for e.g. TenantUsers
    *
    * @return
    */
  def asCaseUserIdentity(): CaseUserIdentity = throw new RuntimeException(s"Cannot convert a ${this.getClass.getName} to a CaseUserIdentity, implementation is missing")
}

object UserIdentity {
  def deserialize(json: ValueMap): UserIdentity = {
    // Note: this is a somewhat "brute-force" deserialization method, mostly required for ModelEvent, ModelResponse and ProcessCommand
    //  It is probably more clean to apply the ModelCommand.readUser also for ModelEvent, but that can be added later.
    //  Similarly, ProcessCommand then should conform to CaseUserIdentity as well.

    if (json.has(Fields.origin)) {
      // Typically from a CaseEvent
      CaseUserIdentity.deserialize(json)
    } else if (json.has(Fields.tenant)) {
      // Classic event structure
      TenantUser.deserialize(json)
    } else if (json.has(Fields.userId)) {
      // Probably DebugEvent deserialization on PlatformTenantCommand
      new UserIdentity {
        override val id: String = json.readString(Fields.userId)
      }
    } else {
      throw new DeserializationError("Cannot deserialize UserIdentity from json " + json)
    }
  }
}
