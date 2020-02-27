/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.service.api.tenant.route

import org.cafienne.infrastructure.akka.http.route.CommandRoute
import org.cafienne.tenant.akka.command.TenantCommand

trait TenantRoute extends CommandRoute {

  def askTenant(command: TenantCommand) = {
    askModelActor(command)
  }
}
