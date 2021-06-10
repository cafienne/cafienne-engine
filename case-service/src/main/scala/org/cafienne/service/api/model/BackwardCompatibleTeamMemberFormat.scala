package org.cafienne.service.api.model

case class BackwardCompatibleTeamMemberFormat(user: Option[String], // Old property, to be ccompatiblty
                                              roles: Option[Seq[String]], // Old property, just keep it here to remain compatible
                                              // New structure below
                                              memberId: Option[String],
                                              memberType: Option[String],
                                              removeRoles: Option[Seq[String]],
                                              caseRoles: Option[Seq[String]],
                                              isOwner: Option[Boolean])
