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

package org.cafienne.actormodel.message;

import org.cafienne.actormodel.ModelActor;
import org.cafienne.actormodel.command.BootstrapMessage;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.CafienneSerializable;
import org.cafienne.json.ValueMap;

/**
 * A UserMessage carries user information
 * Typically used in Commands and resulting Events and Responses from those commands.
 */
public interface UserMessage extends CafienneSerializable {
    UserIdentity getUser();

    /**
     * Explicit method to be implemented returning the type of the ModelActor handling this message.
     * This is required for the message routing within the CaseSystem
     *
     * @return
     */
    default Class<?> actorClass() {
        return ModelActor.class;
    }

    default boolean isBootstrapMessage() {
        return false;
    }

    default BootstrapMessage asBootstrapMessage() {
        return (BootstrapMessage) this;
    }

    default String getDescription() {
        return this.getClass().getSimpleName();
    }

    /**
     * Return a ValueMap serialization of the message
     */
     ValueMap rawJson();

}
