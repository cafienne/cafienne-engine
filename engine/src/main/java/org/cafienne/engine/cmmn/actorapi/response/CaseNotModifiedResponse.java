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

package org.cafienne.engine.cmmn.actorapi.response;

import org.cafienne.actormodel.response.CommandFailure;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.engine.cmmn.actorapi.command.CaseCommand;
import org.cafienne.engine.cmmn.actorapi.command.plan.GetDiscretionaryItems;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * If the case instance has handled an {@link CaseCommand}, it will return a CaseResponse to the sender of the command. This can be used to communicate back
 * e.g. a http message code 200 to a web client.
 * A command response usually contains little content. However, there are some exceptions:
 * <ul>
 * <li>The command actually returns a value that cannot be derived from the event stream, e.g. the list of discretionary items, see {@link GetDiscretionaryItems}</li>
 * <li>The command was erroneous and an exception needs to be returned, see {@link CommandFailure}</li>
 * </ul>
 */
@Manifest
public class CaseNotModifiedResponse extends CaseResponse {
    public CaseNotModifiedResponse(CaseCommand command) {
        super(command);
    }

    public CaseNotModifiedResponse(ValueMap json) {
        super(json);
    }

    @Override
    public String toString() {
        return "CaseNotModifiedResponse for "+getActorId()+": last modified is "+getLastModified();
    }
}
