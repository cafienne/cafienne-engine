/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.cases.route

import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import javax.ws.rs._
import org.cafienne.akka.actor.CaseSystem
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.cases.{CaseQueries, CaseReader}

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContextExecutor

@Api(tags = Array("case"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/cases")
class CasesRoutes(val caseQueries: CaseQueries)(override implicit val userCache: IdentityProvider) extends CasesRoute with CaseReader {

  implicit def executionContext: ExecutionContextExecutor = CaseSystem.system.dispatcher
  val caseRoute = new CaseRoute(caseQueries)(userCache)
  val caseFileRoute = new CaseFileRoute(caseQueries)(userCache)
  val caseTeamRoute = new CaseTeamRoute(caseQueries)(userCache)
  val planItemRoute = new PlanItemRoute(caseQueries)(userCache)
  val discretionaryRoute = new DiscretionaryRoute(caseQueries)(userCache)

  override def routes =
    pathPrefix("cases") {
      caseRoute.routes ~
      caseFileRoute.routes ~
      planItemRoute.routes ~
      discretionaryRoute.routes ~
      caseTeamRoute.routes
    }

  override def apiClasses(): Seq[Class[_]] = {
    Seq(classOf[CaseRoute], classOf[CaseFileRoute], classOf[CaseTeamRoute], classOf[PlanItemRoute], classOf[DiscretionaryRoute])
  }
}
