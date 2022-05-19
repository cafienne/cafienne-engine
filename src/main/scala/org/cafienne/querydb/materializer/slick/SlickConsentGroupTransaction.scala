package org.cafienne.querydb.materializer.slick

import org.cafienne.querydb.materializer.consentgroup.ConsentGroupStorageTransaction
import org.cafienne.querydb.record.{ConsentGroupMemberRecord, ConsentGroupRecord}

class SlickConsentGroupTransaction extends SlickQueryDBTransaction with ConsentGroupStorageTransaction {

  import dbConfig.profile.api._

  override def upsert(record: ConsentGroupRecord): Unit = addStatement(TableQuery[ConsentGroupTable].insertOrUpdate(record))

  override def upsert(record: ConsentGroupMemberRecord): Unit = addStatement(TableQuery[ConsentGroupMemberTable].insertOrUpdate(record))

  override def delete(record: ConsentGroupMemberRecord): Unit = addStatement(TableQuery[ConsentGroupMemberTable].filter(_.group === record.group).filter(_.userId === record.userId).filter(_.role === record.role).delete)

  override def deleteConsentGroupMember(groupId: String, userId: String): Unit = addStatement(TableQuery[ConsentGroupMemberTable].filter(_.group === groupId).filter(_.userId === userId).delete)
}
