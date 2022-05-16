package org.cafienne.querydb.materializer.consentgroup

import org.cafienne.querydb.materializer.QueryDBTransaction
import org.cafienne.querydb.record.{ConsentGroupMemberRecord, ConsentGroupRecord}

trait ConsentGroupStorageTransaction extends QueryDBTransaction {
  def upsert(record: ConsentGroupRecord): Unit

  def upsert(record: ConsentGroupMemberRecord): Unit

  def delete(record: ConsentGroupMemberRecord): Unit

  def deleteConsentGroupMember(groupId: String, userId: String): Unit = ???
}
