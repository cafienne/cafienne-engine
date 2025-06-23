package org.cafienne.persistence.querydb.query.tenant

import org.cafienne.actormodel.identity.{ConsentGroupUser, UserIdentity}
import org.cafienne.consentgroup.actorapi.{ConsentGroup, ConsentGroupMember}

import scala.concurrent.Future

trait ConsentGroupQueries {

  def getConsentGroup(user: UserIdentity, groupId: String): Future[ConsentGroup]

  def getConsentGroups(groupIds: Seq[String]): Future[Seq[ConsentGroup]]

  def getConsentGroupMember(user: UserIdentity, groupId: String, userId: String): Future[ConsentGroupMember]

  def getConsentGroupUser(user: UserIdentity, groupId: String): Future[ConsentGroupUser]

}
