package org.cafienne.service.api.projection.query

import org.cafienne.akka.actor.identity.PlatformUser
import org.cafienne.akka.actor.serialization.json.{StringValue, Value}
import org.cafienne.infrastructure.jdbc.query.{Area, Sort}
import org.cafienne.infrastructure.json.CafienneJson
import org.cafienne.service.api.identifiers.route.IdentifierSet

import scala.concurrent.Future

trait IdentifierQueries {
  def getIdentifiers(user: PlatformUser, filter: IdentifierFilter, area: Area = Area.Default, sort: Sort = Sort.NoSort): Future[IdentifierSet] = ???

  def getIdentifierNames(user: PlatformUser, tenant: Option[String]): Future[Seq[IdentifierName]] = ???
}

class IdentifierQueriesImpl
  extends IdentifierQueries
    with BaseQueryImpl {

  import dbConfig.profile.api._

  override def blankIdentifierFilterQuery(caseInstanceId: Rep[String]): Query[_, _, Seq] = {
    caseIdentifiersQuery.filter(_.caseInstanceId === caseInstanceId)
  }

  override def getIdentifiers(user: PlatformUser, filter: IdentifierFilter, area: Area, sort: Sort): Future[IdentifierSet] = {
    val query = for {
      baseQuery <- caseIdentifiersQuery
        .filter(_.active === true)
        .filterOpt(filter.tenant)((identifier, tenant) => identifier.tenant === tenant)
        .filterOpt(filter.name)((identifier, name) => identifier.name === name)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery
    db.run(query.distinct.only(area).order(sort).result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      IdentifierSet(records)
    })
  }

  override def getIdentifierNames(user: PlatformUser, tenant: Option[String]): Future[Seq[IdentifierName]] = {
    val query = for {
      baseQuery <- caseIdentifiersQuery
        .filter(_.active === true)
        .filterOpt(tenant)((identifier, tenant) => identifier.tenant === tenant)
      // Validate team membership
      _ <- membershipQuery(user, baseQuery.caseInstanceId, baseQuery.tenant, None)
    } yield baseQuery.name
    db.run(query.distinct.result).map(records => {
      //      println("Found " + records.length +" matching cases on filter " + identifiers)
      records.map(IdentifierName)
    })
  }
}

case class IdentifierName(name: String) extends CafienneJson {
  override def toValue: Value[_] = new StringValue(name)
}