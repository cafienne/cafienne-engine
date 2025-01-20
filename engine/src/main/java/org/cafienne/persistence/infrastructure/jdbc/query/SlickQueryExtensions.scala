package org.cafienne.persistence.infrastructure.jdbc.query

import org.cafienne.persistence.infrastructure.jdbc.SlickTableExtensions

trait SlickQueryExtensions extends SlickTableExtensions {
  import dbConfig.profile.api._

  implicit class QueryHelper[CT <: CafienneTable[_], E](query: Query[CT, E, Seq]) {
    /**
     * Orders the results as given in the Sort object.
     * Note that if the Sort.on field is empty (None), the query will not be affected.
     * Default sort order is descending
     */
    def order(sort: Sort): Query[CT, E, Seq] = {
      sort.on.fold(query)(fieldName => if (sort.ascending) {
        query.sortBy(_.getSortColumn(fieldName.toLowerCase).asc)
      } else {
        query.sortBy(_.getSortColumn(fieldName.toLowerCase).desc)
      })
    }

    /**
     * Only select records from a certain offset and up to a certain number of results
     */
    def only(area: Area): Query[CT, E, Seq] = {
      query.drop(area.offset).take(area.numOfResults)
    }
  }

  implicit class TenantQueryHelper[CTT <: CafienneTenantTable[_], E](query: Query[CTT, E, Seq]) {
    /**
     * Add tenant selector to the query. If no tenants specified, it will not add a filter on tenant
     * and search across tenants
     */
    def inTenants(tenants: Option[Seq[String]]): Query[CTT, E, Seq] = {
      val set = tenants.getOrElse(Seq())
      if (set.isEmpty) {
        query
      } else {
        query.filter(_.tenant.inSet(set))
      }
    }
  }
}
