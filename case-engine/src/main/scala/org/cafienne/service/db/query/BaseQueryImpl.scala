package org.cafienne.service.db.query

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.actormodel.identity.{ConsentGroupMembership, PlatformUser}
import org.cafienne.service.db.record.{CaseBusinessIdentifierRecord, CaseRecord}
import org.cafienne.service.db.schema.table.{CaseTables, ConsentGroupTables, TaskTables, TenantTables}

trait BaseQueryImpl
  extends CaseTables
    with TaskTables
    with TenantTables
    with ConsentGroupTables
    with LazyLogging {

  import dbConfig.profile.api._

  implicit val ec = db.ioExecutionContext // TODO: Is this the best execution context to pick?

  val caseInstanceQuery = TableQuery[CaseInstanceTable]
  val caseDefinitionQuery = TableQuery[CaseInstanceDefinitionTable]
  val caseFileQuery = TableQuery[CaseFileTable]
  val caseIdentifiersQuery = TableQuery[CaseBusinessIdentifierTable]

  val planItemTableQuery = TableQuery[PlanItemTable]

  /**
    * Query that validates that the user belongs to the team of the specified case, either by explicit
    * membership of the user id, or by one of the tenant roles of the user that are bound to the team of the case
    * @param user
    * @param caseInstanceId
    * @param tenant
    * @return
    */
  def membershipQuery(user: PlatformUser, caseInstanceId: Rep[String]): Query[CaseInstanceTable, CaseRecord, Seq] = {
    val groupMembership = TableQuery[ConsentGroupMemberTable].filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamGroupTable].filter(_.caseInstanceId === caseInstanceId))
      .on((group, member) => {
        // User belongs to the case team if the group belongs to the case team and either:
        // - the user has a group role matching the case membership's group role
        // - or the user is group owner
        group.group === member.groupId && (group.role === member.groupRole || group.isOwner)
      })
      .map(_._2.caseInstanceId)

    val tenantRoleBasedMembership = TableQuery[UserRoleTable].filter(_.userId === user.id)
      .join(TableQuery[CaseInstanceTeamTenantRoleTable].filter(_.caseInstanceId === caseInstanceId))
      // The tenant role must be in the case team, and also the user must have the role in the same tenant
      .on((left, right) => left.role_name === right.tenantRole && left.tenant === right.tenant)
      .map(_._2.caseInstanceId)

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
    * @param platformUser
    * @param caseInstanceId
    * @param identifiers
    * @return
    */
  def membershipQuery(platformUser: PlatformUser, caseInstanceId: Rep[String], identifiers: Option[String]): Query[CaseInstanceTable, CaseRecord, Seq] = {
    if (identifiers.isEmpty) membershipQuery(platformUser, caseInstanceId)
    else for {
      teamMemberShip <- membershipQuery(platformUser, caseInstanceId)
      _ <- new BusinessIdentifierFilterParser(identifiers).asQuery(caseInstanceId)
    } yield teamMemberShip
  }

  def createCaseUserIdentity(user: PlatformUser, userRecords: Set[(String, String, String)], groupRecords: Set[(String, String, String)], tenantRoleRecords: Set[(String, String, String)], exception: String => Exception, msg: String): CaseMembership = {
    if (userRecords.isEmpty && groupRecords.isEmpty && tenantRoleRecords.isEmpty) {
      // All rows empty
      throw exception(msg)
    }

    val caseId = {
      if (userRecords.nonEmpty) userRecords.head._1
      else if (groupRecords.nonEmpty) groupRecords.head._1
      else if (tenantRoleRecords.nonEmpty) tenantRoleRecords.head._1
      else throw exception(msg) // Pretty weird, as we just above checked that at least one of the Sets is nonEmpty
    }

    val tenantId: String = {
      if (userRecords.nonEmpty) userRecords.head._2
      else if (groupRecords.nonEmpty) groupRecords.head._2
      else if (tenantRoleRecords.nonEmpty) tenantRoleRecords.head._2
      else throw exception(msg) // Pretty weird, as we just above checked that at least one of the Sets is nonEmpty
    }

    val userIdBasedMembership: Set[String] = userRecords.map(_._3)
    val groupBasedMembership: Seq[ConsentGroupMembership] = groupRecords.map(_._3).map(user.group).toSeq.filter(_ != null)
    val tenantRoleBasedMembership: Set[String] = tenantRoleRecords.map(_._3)
    // Not all tenant roles of the case team may apply to this user, only those that the user actually has in the tenant ...
    val userTenantRoles: Set[String] = user.tenantRoles(tenantId).intersect(tenantRoleBasedMembership)
    // ... and, if those are non empty only then we have an actual access to this case
    if (userIdBasedMembership.isEmpty && groupBasedMembership.isEmpty && userTenantRoles.isEmpty) throw exception(msg)

    new CaseMembership(id = user.id, origin = user.origin(tenantId), tenantRoles = userTenantRoles, groups = groupBasedMembership, caseInstanceId = caseId, tenant = tenantId)
  }

  class BusinessIdentifierFilterParser(string: Option[String]) {
    private val filters: Seq[ParsedFilter] = string.fold(Seq[ParsedFilter]()) {
      parseFilters
    }

    def asQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]  = {
      val topLevelQuery = filters.length match {
        case 0 => {
          // If no filter is specified, then there must be at least something in the business identifier table, i.e.,
          //  at least one business identifier must be filled in the case.
          TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseInstanceId)
        }
        case 1 => {
          logger.whenDebugEnabled{logger.debug(s"Simple filter: [$string]")}
          filters.head.toQuery(caseInstanceId)
        }
        case moreThanOne => {
          logger.whenDebugEnabled{logger.debug(s"Composite filter on $moreThanOne fields: [$string]")}
          for {
            topQuery <- filters.head.toQuery(caseInstanceId)
            _ <- createCompositeQuery(1, topQuery.caseInstanceId)
          } yield topQuery
        }
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
            } yield f1      *
      * @param current
      * @param caseInstanceId
      * @return
      */
    def createCompositeQuery(current: Int, caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
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

    override def toString: String = {
      s"====================== Filter[${string}]\n${filters.map(filter => s"Filter[${filter.field}]: $filter").mkString("\n")}\n========================"
    }

    def parseFilters(query: String): Seq[ParsedFilter] = {
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

  trait RawFilter {
    protected val rawFieldName: String // Raw field name should NOT be used, only the trimmed version should be used.
    lazy val field: String = rawFieldName.trim() // Always trim field names.
  }

  trait BasicValueFilter extends RawFilter {
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

  case class NotValueFilter(rawFilter: String, splitter: String = "!=") extends BasicValueFilter

  case class ValueFilter(rawFilter: String, splitter: String = "=") extends BasicValueFilter

  trait ParsedFilter extends RawFilter {
    def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq]
  }

  case class NoFilter(rawFieldName: String = "") extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]) = TableQuery[CaseBusinessIdentifierTable].filter(_.caseInstanceId === caseInstanceId)
  }

  case class NotFieldFilter(rawFieldName: String) extends ParsedFilter {
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

  case class FieldFilter(rawFieldName: String) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = {
      logger.whenDebugEnabled{logger.debug(s"Adding 'Field-must-be-set' filter for field $field")}
      TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value.nonEmpty)
    }
  }

  case class JoinedNotFilter(rawFieldName: String, values: Seq[String]) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = values.length match {
      case 1 => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Field-does-not-have-value' filter $field == ${values(0)}")}
        TableQuery[CaseBusinessIdentifierTable]
          .filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field)
          .filterNot(_.value === values(0))
      }
      case _ => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Value-NOT-in-set' filter for field $field on values $values")}
        TableQuery[CaseBusinessIdentifierTable].filterNot(record => record.caseInstanceId === caseInstanceId && record.active === true && record.name === field && record.value.inSet(values))
      }
    }
  }

  case class OrFilter(rawFieldName: String, values: Seq[String]) extends ParsedFilter {
    override def toQuery(caseInstanceId: Rep[String]): Query[CaseBusinessIdentifierTable, CaseBusinessIdentifierRecord, Seq] = values.length match {
      case 1 => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Field-has-value' filter $field == ${values(0)}")}
        TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value === values(0))
      }
      case _ => {
        logger.whenDebugEnabled{logger.debug(s"Adding 'Value-in-set' filter for field $field on values $values")}
        TableQuery[CaseBusinessIdentifierTable].filter(identifier => identifier.caseInstanceId === caseInstanceId && identifier.active === true && identifier.name === field && identifier.value.inSet(values))
      }
    }
  }

}

