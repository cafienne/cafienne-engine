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

package com.casefabric.actormodel.response;

import com.casefabric.actormodel.ModelActor;
import com.casefabric.actormodel.command.ModelCommand;

/**
 * When sending a message to another model instance from within a model,
 * the method {@link ModelActor#askModel(ModelCommand, CommandFailureListener, CommandResponseListener...)} can be used.
 * The case instance will respond to "this" case and this case will invoke the registered response listener.
 * This basically supports a simplified ask pattern between cases.
 *
 */
@FunctionalInterface
public interface CommandFailureListener {
    /**
     * The handleFailure method can be implemented to handle {@link CommandFailure} coming back as a result from sending a command to the other model that could not be handled.
     * @param failure
     */
    void handleFailure(CommandFailure failure);
}
