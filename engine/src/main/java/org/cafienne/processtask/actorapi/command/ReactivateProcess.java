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

package org.cafienne.processtask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessReactivated;
import org.cafienne.processtask.actorapi.event.ProcessStarted;
import org.cafienne.processtask.actorapi.response.ProcessResponse;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.io.IOException;

@Manifest
public class ReactivateProcess extends ProcessCommand {
    private final ValueMap inputParameters;

    public ReactivateProcess(UserIdentity user, String id, ValueMap inputParameters) {
        super(user, id);
        this.inputParameters = inputParameters;
    }

    public ReactivateProcess(ValueMap json) {
        super(json);
        this.inputParameters = json.readMap(Fields.inputParameters);
    }

    public ValueMap getInputParameters() {
        return inputParameters;
    }

    @Override
    protected void process(ProcessTaskActor processTaskActor, SubProcess<?> implementation) {
        processTaskActor.addEvent(new ProcessReactivated(processTaskActor, this));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelCommand(generator);
        writeField(generator, Fields.inputParameters, inputParameters);
    }
}
