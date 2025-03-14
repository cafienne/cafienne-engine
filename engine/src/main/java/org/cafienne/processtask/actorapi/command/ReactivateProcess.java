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

import org.cafienne.actormodel.identity.UserIdentity;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.actorapi.event.ProcessReactivated;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;

@Manifest
public class ReactivateProcess extends StartProcess {
    public ReactivateProcess(UserIdentity user, String tenant, String id, String name, ProcessDefinition definition, ValueMap inputParameters, String parentActorId, String rootActorId, boolean debugMode) {
        super(user, tenant, id, name, definition, inputParameters, parentActorId, rootActorId, debugMode);
    }

    public ReactivateProcess(ValueMap json) {
        super(json);
    }

    @Override
    protected void process(ProcessTaskActor processTaskActor, SubProcess<?> implementation) {
        if (processTaskActor.getDefinition() == null) {
            super.process(processTaskActor, implementation);
        } else {
            processTaskActor.addEvent(new ProcessReactivated(processTaskActor, this));
        }
    }
}
