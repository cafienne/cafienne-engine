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

package com.casefabric.journal.jdbc

import com.typesafe.scalalogging.LazyLogging
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.persistence.jdbc.db.DefaultSlickDatabaseProvider

/**
  * This class is no longer needed, but it may still be configured in existing installations.
  *
  * @deprecated This class is no longer in use
  *
  */
class EventDatabaseProvider(system: ActorSystem) extends DefaultSlickDatabaseProvider(system) with LazyLogging {
  logger.warn("Kindly remove the configuration property 'database-provider-fqcn = \"com.casefabric.journal.jdbc.EventDatabaseProvider\n' as this is no longer supported and will be dropped in a future release")
}
