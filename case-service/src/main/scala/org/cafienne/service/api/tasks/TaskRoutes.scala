/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tasks

import akka.http.scaladsl.server.Directives._
import io.swagger.annotations._
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import javax.ws.rs.Path
import org.cafienne.identity.IdentityProvider
import org.cafienne.service.api.projection.query.TaskQueries

import scala.collection.immutable.Seq

@Api(tags = Array("tasks"))
@SecurityRequirement(name = "openId", scopes = Array("openid"))
@Path("/tasks")
class TaskRoutes(val taskQueries: TaskQueries)(override implicit val userCache: IdentityProvider) extends TaskRoute {
  val taskQueryRoute = new TaskQueryRoutes(taskQueries)(userCache)
  val taskActionRoute = new TaskActionRoutes(taskQueries)(userCache)

  override def routes = pathPrefix("tasks") {
    taskQueryRoute.routes ~ taskActionRoute.routes
  }

  override def apiClasses(): Seq[Class[_]] = {
    Seq(classOf[TaskQueryRoutes], classOf[TaskActionRoutes])
  }
}
