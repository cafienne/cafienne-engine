package org.cafienne.persistence.querydb.query.cmmn.identifier

import org.cafienne.actormodel.identity.UserIdentity
import org.cafienne.persistence.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.persistence.querydb.query.cmmn.filter.IdentifierFilter
import org.cafienne.persistence.querydb.query.cmmn.implementations.BaseQueryImpl
import org.cafienne.persistence.querydb.schema.QueryDB

import scala.concurrent.Future

class IdentifierQueriesImpl(queryDB: QueryDB)
  extends BaseQueryImpl(queryDB)
    with IdentifierQueries {

  import dbConfig.profile.api._

  override def getIdentifiers(user: UserIdentity, filter: IdentifierFilter, area: Area, sort: Sort): Future[IdentifierSet] = {
    val query = for {
      baseQuery <- caseIdentifiersQuery
        .filter(_.active === true)
        .filterOpt(filter.tenant)((identifier, tenant) => identifier.tenant === tenant)
        .filterOpt(filter.name)((identifier, name) => identifier.name === name)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery
    db.run(query.distinct.only(area).order(sort).result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      IdentifierSet(records)
    })
  }

  override def getIdentifierNames(user: UserIdentity, tenant: Option[String]): Future[Seq[IdentifierName]] = {
    val query = for {
      baseQuery <- caseIdentifiersQuery
        .filter(_.active === true)
        .filterOpt(tenant)((identifier, tenant) => identifier.tenant === tenant)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId)
    } yield baseQuery.name
    db.run(query.distinct.result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      records.map(IdentifierName)
    })
  }
}
