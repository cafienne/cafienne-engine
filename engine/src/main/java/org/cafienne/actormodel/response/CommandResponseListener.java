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

package org.cafienne.actormodel.response;

import org.cafienne.engine.cmmn.actorapi.command.CaseCommand;
import org.cafienne.engine.cmmn.actorapi.response.CaseResponse;
import org.cafienne.engine.cmmn.instance.Case;

/**
 * When sending a message to another model instance from within a model,
 * the method {@link Case#askCase(CaseCommand, CommandFailureListener, CommandResponseListener...)} can be used.
 * The case instance will respond to "this" case and this case will invoke the registered response listener.
 * This basically supports a simplified ask pattern between cases.
 *
 */
@FunctionalInterface
public interface CommandResponseListener {
    /**
     * The handleResponse method can be implemented to handle a valid {@link CaseResponse} coming back as a result of sending a command to another model.
     * @param response
     */
    void handleResponse(ModelResponse response);
}
