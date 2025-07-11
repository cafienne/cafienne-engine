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

package org.cafienne.engine.processtask.actorapi.command;

import org.cafienne.actormodel.command.BaseModelCommand;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.json.ValueMap;
import org.cafienne.engine.processtask.actorapi.ProcessActorMessage;
import org.cafienne.engine.processtask.actorapi.response.ProcessResponse;
import org.cafienne.engine.processtask.implementation.SubProcess;
import org.cafienne.engine.processtask.instance.ProcessTaskActor;

public abstract class ProcessCommand extends BaseModelCommand<ProcessTaskActor, UserIdentity> implements ProcessActorMessage {
    protected ProcessCommand(UserIdentity user, String id) {
        super(user, id);
    }

    protected ProcessCommand(ValueMap json) {
        super(json);
    }

    @Override
    protected UserIdentity readUser(ValueMap json) {
        return UserIdentity.deserialize(json);
    }

    @Override
    public void validate(ProcessTaskActor modelActor) throws InvalidCommandException {
        // Nothing to validate
    }

    @Override
    public void process(ProcessTaskActor processTaskActor) {
        process(processTaskActor, processTaskActor.getImplementation());
        if (hasNoResponse()) { // Always return a response
            setResponse(new ProcessResponse(this));
        }
    }

    abstract protected void process(ProcessTaskActor processTaskActor, SubProcess<?> implementation);
}
