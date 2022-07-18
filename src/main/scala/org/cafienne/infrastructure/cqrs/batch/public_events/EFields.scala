/*
 * Copyright 2014 - 2022 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.cafienne.infrastructure.cqrs.batch.public_events

/**
 * Sort of an extension to  {@link org.cafienne.infrastructure.serialization.Fields}
 *
 */
object EFields {
  val parentCaseId = "parentCaseId"
  val rootCaseId = "rootCaseId"
  val file = "file"
  val form = "form"
}
