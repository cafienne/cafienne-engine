/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.querydb.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{ConsentGroupMembership, Origin, UserIdentity}
import org.cafienne.querydb.record._
import org.cafienne.querydb.schema.table.{CaseTables, ConsentGroupTables, TaskTables, TenantTables}

import scala.concurrent.{ExecutionContext, Future}

trait BaseQueryImpl
  extends CaseTables
    with TaskTables
    with TenantTables
    with ConsentGroupTables
    with LazyLogging {

  import dbConfig.profile.api._

  implicit val ec: ExecutionContext = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val caseInstanceQuery = TableQuery[CaseInstanceTable]
  val caseDefinitionQuery = TableQuery[CaseInstanceDefinitionTable]
  val caseFileQuery = TableQuery[CaseFileTable]
  val caseIdentifiersQuery = TableQuery[CaseBusinessIdentifierTable]

  val planItemTableQuery = TableQuery[PlanItemTable]

  def getCaseMembership(caseInstanceId: String, user: UserIdentity, exception: String => Exception, msg: String): Future[CaseMembership] = {
//    if (msg == caseInstanceId) {
//      println(s"Trying to fetch case '$caseInstanceId' ")
//    } else {
//      println(s"Trying to fetch case '$caseInstanceId' for task '$msg'")
//    }

    def fail: String = throw exception(msg) // Fail when either caseId or tenantId remains to be empty

    val originQuery = {
      val tenantUser =
        TableQuery[CaseInstanceTable]
          .filter(_.id === caseInstanceId)
          .joinLeft(TableQuery[UserRoleTable]
            .filter(_.userId === user.id)
            .filter(_.role_name === "")
            .map(user => (user.userId, user.tenant)))
          .on(_.tenant === _._2).map(join => (join._1.id, join._1.tenant, join._2))
      val platformUser = TableQuery[UserRoleTable].filter(_.userId === user.id).filter(_.role_name === "").map(_.userId).take(1)

      tenantUser.joinFull(platformUser)
    }

    val groupMembership = TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId)
      .join(TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id))
      .on((caseGroup, group) => caseGroup.groupId === group.group && (caseGroup.groupRole === group.role || group.isOwner))
      .map(_._2).map(group => (group.group, group.isOwner, group.role)) // Note: we need GROUP ownership, not case team ownership!!!

    val tenantRoleBasedMembership = tenantRoleBasedMembershipQuery(caseInstanceId, user).map(_.tenantRole)

    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.userId === user.id)
      .filter(_.caseInstanceId === caseInstanceId)
      .map(_.userId)

    val query =
      originQuery.joinLeft(
        // Note: order matters. Putting group membership at the end generates an invalid SQL statement
        //  guess that's some kind of issue in Slick
        groupMembership.joinFull(tenantRoleBasedMembership.joinFull(userIdBasedMembership)))

//    println("CASE MEMBERSHIP QUERY:\n\n" + query.result.statements.mkString("\n")+"\n\n")
    val records = db.run(query.distinct.result)

    records.map(x => {
      if (x.isEmpty) {
//        println(" Failing because there records are not found")
        fail
      }

      val originRecords = x.map(_._1)//filter(_.isDefined).map(_.get)
      if (originRecords.headOption.isEmpty) {
//        println(" Failing because head option is empty")
        fail // Case does not exist
      }

      if (originRecords.head._1.isEmpty) {
//        println(" Failing because head._1 is empty")
        fail // Again, case apparently does not exist (then why do we have a head in the first place ??? Perhaps it is filled with all NULL values???
      }

      val caseId = originRecords.head._1.get._1
      val tenantId = originRecords.head._1.get._2
//      println(" Case id: " + caseId)
//      println(" Tenant id: " + tenantId)
      val origin = {
        if (originRecords.isEmpty) Origin.IDP // No platform registration for this user id
        else if (originRecords.head._1.get._3.isDefined) Origin.Tenant
        else if (originRecords.head._2.isDefined) Origin.Platform
        else Origin.IDP // Just a default, should not reach this statement at all.
      }
//      println(s" User ${user.id} has origin $origin")



      val membershipRecords: Seq[(Option[(String, Boolean, String)], Option[(Option[String], Option[String])])] = x.map(_._2).filter(_.isDefined).map(_.get)
      val userAndRoleRecords = membershipRecords.map(_._2).filter(_.isDefined).map(_.get)
      val userIdBasedMembership: Set[String] = userAndRoleRecords.map(_._2).filter(_.isDefined).map(_.get).toSet
//      println(s"Found ${userRecords.size} user records")

      val userTenantRoles: Set[String] = userAndRoleRecords.map(_._1).filter(_.isDefined).map(_.get).toSet
//      println(s"Found ${tenantRoleRecords.size} tenant role records")
      val groupRecords: Set[ConsentGroupMemberRecord] = membershipRecords.map(_._1).filter(_.isDefined).map(_.get).map(group => ConsentGroupMemberRecord(group = group._1, userId = user.id, isOwner = group._2, role = group._3)).toSet
//      println(s"Found ${groupRecords.size} group records")

      val groups = groupRecords.map(_.group)
      val groupBasedMembership: Seq[ConsentGroupMembership] = groups.map(groupId => {
        val groupElements = groupRecords.filter(_.group == groupId)
        val isOwner = groupElements.exists(_.isOwner)
        val roles = groupElements.map(_.role)
        ConsentGroupMembership(groupId, roles, isOwner)
      }).toSeq

      // ... and, if those are non empty only then we have an actual access to this case
      if (userIdBasedMembership.isEmpty && groupBasedMembership.isEmpty && userTenantRoles.isEmpty) {
        // All rows empty, no access to this case
//        println("Failing because membership sets are empty")
        fail
      }

      new CaseMembership(id = user.id, origin = origin, tenantRoles = userTenantRoles, groups = groupBasedMembership, caseInstanceId = caseId, tenant = tenantId)

    })
  }

  def tenantRoleBasedMembershipQuery(caseInstanceId: Rep[String], user: UserIdentity): Query[CaseInstanceTeamTenantRoleTable, CaseTeamTenantRoleRecord, Seq] = {
    tenantRoleQuery(user)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseInstanceId))
      // The tenant role must be in the case team, and also the user must have the role in the same tenant
      .on((left, right) => (left._1 === right.tenantRole) && left._2 === right.tenant)
      .map(_._2)
  }

  def tenantRoleQuery(user: UserIdentity): Query[(Rep[String], Rep[String]), (String, String), Seq] = {
    val groupMemberships = TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[ConsentGroupTable])
      .on(_.group === _.id)
      .map(record => (record._1.role, record._2.tenant))
    val tenantMemberships = TableQuery[UserRoleTable].filter(_.userId === user.id).map(record => (record.role_name, record.tenant))

    tenantMemberships.union(groupMemberships)
  }

  /**
    * Query that validates that the user belongs to the team of the specified case, either by explicit
    * membership of the user id, or by one of the tenant roles of the user that are bound to the team of the case
    */
  def membershipQuery(user: UserIdentity, caseInstanceId: Rep[String]): Query[CaseInstanceTable, CaseRecord, Seq] = {
    val groupMembership = TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId))
      .on((group, member) => {
        // User belongs to the case team if the group belongs to the case team and either:
        // - the user has a group role matching the case membership's group role
        // - or the user is group owner
        group.group === member.groupId && (group.role === member.groupRole || group.isOwner)
      })
      .map(_._2.caseInstanceId)

    val tenantRoleBasedMembership = tenantRoleBasedMembershipQuery(caseInstanceId, user).map(_.caseInstanceId)

    val userIdBasedMembership = TableQuery[CaseInstanceTeamUserTable]
      .filter(_.caseInstanceId === caseInstanceId)
      .filter(_.userId === user.id)
      .map(_.caseInstanceId)

    // Return a filter on the case that also matches membership existence somewhere
    caseInstanceQuery
      .filter(_.id === caseInstanceId)
      .filter(_ => userIdBasedMembership.exists || tenantRoleBasedMembership.exists || groupMembership.exists)
  }

  /**
    * Query that validates that the user belongs to the team of the specified case,
    * and adds an optional business identifiers filter to the query.
    */
  def membershipQuery(user: UserIdentity, caseInstanceId: Rep[String], identifiers: Option[String]): Query[CaseInstanceTable, CaseRecord, Seq] = {
    if (identifiers.isEmpty) membershipQuery(user, caseInstanceId)
    else for {
      teamMemberShip <- membershipQuery(user, caseInstanceId)
      _ <- new BusinessIdentifierFilterParser(identifiers).asQuery(caseInstanceId)
    } yield teamMemberShip
  }

  class BusinessIdentifierFilterParser(string: Option[String]) {
    private val filters: Seq[ParsedFilter] = string.fold(Seq[ParsedFilter]()) {
      parseFilters
    }

    def asQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]  = {
      val topLevelQuery = filters.length match {
        case 0 =>
          // If no filter is specified, then there must be at least something in the business identifier table, i.e.,
          //  at least one business identifier must be filled in the case.
          TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseInstanceId)
        case 1 =>
          logger.whenDebugEnabled{logger.debug(s"Simple filter: [$string]")}
          filters.head.toQuery(caseInstanceId)
        case moreThanOne =>
          logger.whenDebugEnabled{logger.debug(s"Composite filter on $moreThanOne fields: [$string]")}
          for {
            topQuery <- filters.head.toQuery(caseInstanceId)
            _ <- createCompositeQuery(1, topQuery.caseInstanceId)
          } yield topQuery
      }
      topLevelQuery
    }

    /**
      * Note: this method is recursive, iterating into the depth of the filter list to create a structure like below
            f0 <- getQ(0, caseInstanceId)
            q1 <- for {
              f1 <- getQ(1, f0.caseInstanceId)
              q2 <- for {
                f2 <- getQ(2, f1.caseInstanceId)
                q3 <- for {
                        f3 <- getQ(3, f2.caseInstanceId)
                        q4 <- for {
                            f4 <- getQ(4, f3.caseInstanceId)
                            q5 <- for {
                                f5 <- getQ(5, q4.caseInstanceId)
                                q6 <- for {
                                    f6 <- getQ(6, f5.caseInstanceId)
                                } yield f6
                            } yield f5
                        } yield f4
                    } yield f3
                } yield f2
            } yield f1
      *
      */
    private def createCompositeQuery(current: Int, caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      val next = current + 1
      if (filters.size <= next) {
        for {
          finalQuery <- filters(current).toQuery(caseInstanceId)
        } yield finalQuery
      } else {
        for {
          thisFilterQuery <- filters(current).toQuery(caseInstanceId)
          _ <- createCompositeQuery(next, thisFilterQuery.caseInstanceId)
        } yield thisFilterQuery
      }
    }

    override def toString: String = s"====================== Filter[$string]\n${filters.map(filter => s"Filter[${filter.field}]: $filter").mkString("\n")}\n========================"

    private def parseFilters(query: String): Seq[ParsedFilter] = {
      // First, create a raw list of all filters given.
      val rawFilters: Seq[RawFilter] = query.split(',').toSeq.map(rawFilter => {
        if (rawFilter.isBlank) NoFilter()
        else if (rawFilter.startsWith("!")) NotFieldFilter(rawFilter.substring(1))
        else if (rawFilter.indexOf("!=") > 0) NotValueFilter(rawFilter)
        else if (rawFilter.indexOf("=") > 0) ValueFilter(rawFilter)
        else FieldFilter(rawFilter) // Well, with all options coming
      })

      // Next, collect and merge filters that work on the same field
      val filtersPerField = scala.collection.mutable.LinkedHashMap[String, scala.collection.mutable.ArrayBuffer[RawFilter]]()
      rawFilters.map(filter => filtersPerField.getOrElseUpdate(filter.field, scala.collection.mutable.ArrayBuffer[RawFilter]()) += filter)

      // Next, join filters on the same field to one new BasicFilter for that field
      //  Combination logic:
      //  1. NotFieldFilter takes precedence over all other filters for that field
      //  2. Any NotValueFilter in combination with ValueFilter can be discarded
      def combineToBasicFilter(field: String, filters: Seq[RawFilter]): ParsedFilter = {
        val filter: ParsedFilter = {
          filters.find(f => f.isInstanceOf[NotFieldFilter]).getOrElse({
            val notFieldValues = JoinedNotFilter(field, filters.filter(f => f.isInstanceOf[NotValueFilter]).map(f => f.asInstanceOf[NotValueFilter].value))
            if (notFieldValues.values.nonEmpty) { // There are NotValueFilters; but they are only relevant if there are not also ValueFilters, otherwise ValueFilters take precedence
              val valueFilters = filters.filter(f => f.isInstanceOf[ValueFilter])
              if (valueFilters.nonEmpty) {
                OrFilter(field, valueFilters.map(f => f.asInstanceOf[ValueFilter].value))
              } else {
                // There are no ValueFilters; there might be a FieldFilter, but that can be safely ignored
                notFieldValues
              }
            } else {
              // Check to see if there is a generic FieldFilter, that takes precedence over any ValueFilters for that field
              filters.find(f => f.isInstanceOf[FieldFilter]).getOrElse(OrFilter(field, filters.map(f => f.asInstanceOf[ValueFilter].value)))
            }
          })
        }.asInstanceOf[ParsedFilter]
        filter
      }

      // TODO: for performance reasons we can sort the array to have the "NOT" filters at the end
      filtersPerField.toSeq.map(fieldFilter => combineToBasicFilter(fieldFilter._1, fieldFilter._2.toSeq))
    }
  }

  private trait RawFilter {
    protected val rawFieldName: String // Raw field name should NOT be used, only the trimmed version should be used.
    lazy val field: String = rawFieldName.trim() // Always trim field names.
  }

  private trait BasicValueFilter extends RawFilter {
    private lazy val splittedRawFilter = rawFilter.split(splitter)
    val splitter: String
    val rawFilter: String
    val rawFieldName: String = getContent(0)
    val value: String = getContent(1)

    private def getContent(index: Int): String = {
      if (splittedRawFilter.length > index) splittedRawFilter(index)
      else ""
    }
  }

  private case class NotValueFilter(rawFilter: String, splitter: String = "!=") extends BasicValueFilter

  private case class ValueFilter(rawFilter: String, splitter: String = "=") extends BasicValueFilter

  private trait ParsedFilter extends RawFilter {
    def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]
  }

  private case class NoFilter(rawFieldName: String = "") extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseInstanceId)
  }

  private case class NotFieldFilter(rawFieldName: String) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      logger.warn(s"OPERATION NOT YET SUPPORTED: 'Field-must-NOT-be-set' filter for field $field")
      logger.whenDebugEnabled{logger.debug(s"Adding 'Field-must-NOT-be-set' filter for field $field")}
      // TODO: this must be refactored to support some form of not exists like below. Currently unclear how to achieve this in Slick
      //select case-id
      //from business-identifier 'bi-1'
      //where (bi-1.name === 'd' && bi-1.value in ('java', 'sql')
      //and not exists (select * from business-identifier 'bi-2'
      //                where bi-2.case-id = bi-1.case-id
      //                and bi-2.name = 'u')
      TableQuery[CaseBusinessIdentifierTable].filterNot(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field)
    }
  }

  private case class FieldFilter(rawFieldName: String) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      logger.whenDebugEnabled{logger.debug(s"Adding 'Field-must-be-set' filter for field $field")}
      TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value.nonEmpty)
    }
  }

  private case class JoinedNotFilter(rawFieldName: String, values: Seq[String]) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = values.length match {
      case 1 =>
        logger.whenDebugEnabled{logger.debug(s"Adding 'Field-does-not-have-value' filter $field == ${values.head}")}
        TableQuery[CaseBusinessIdentifierTable]
          .filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field)
          .filterNot(_.value === values.head)
      case _ =>
        logger.whenDebugEnabled{logger.debug(s"Adding 'Value-NOT-in-set' filter for field $field on values $values")}
        TableQuery[CaseBusinessIdentifierTable].filterNot(record => record.caseInstanceId === caseInstanceId && record.active === true && record.name === field && record.value.inSet(values))
    }
  }

  private case class OrFilter(rawFieldName: String, values: Seq[String]) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = values.length match {
      case 1 =>
        logger.whenDebugEnabled{logger.debug(s"Adding 'Field-has-value' filter $field == ${values.head}")}
        TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value === values.head)
      case _ =>
        logger.whenDebugEnabled{logger.debug(s"Adding 'Value-in-set' filter for field $field on values $values")}
        TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value.inSet(values))
    }
  }

}

