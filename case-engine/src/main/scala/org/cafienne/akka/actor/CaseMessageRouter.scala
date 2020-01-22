/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.akka.actor

import akka.actor.ActorRef

/**
  * Simple service that can route case commands to the correct case instance
  */
trait MessageRouterService {
  def getCaseMessageRouter(): ActorRef
  def getTenantMessageRouter(): ActorRef
  def getProcessMessageRouter(): ActorRef
}

