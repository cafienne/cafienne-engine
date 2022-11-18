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

package org.cafienne.actormodel.command;

import org.cafienne.actormodel.message.UserMessage;

/**
 * The first command that is sent to a ModelActor has to implement this interface such that the actor can
 * initialize itself with the required information.
 * This is required to enable the ModelActor class to do some basic authorization checks that must be done by
 * the platform and cannot be left to actor specific logic overwriting it.
 * It should also be implemented in the first state-changing ModelEvent, so that the same information can be set
 * during recovery of the ModelActor
 */
public interface BootstrapMessage extends UserMessage {
    String tenant();

    @Override
    default boolean isBootstrapMessage() {
        return true;
    }
}
