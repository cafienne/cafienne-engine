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

package org.cafienne.engine.processtask.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.event.BaseModelEvent;
import org.cafienne.json.ValueMap;
import org.cafienne.engine.processtask.instance.ProcessTaskActor;

import java.io.IOException;

public abstract class BaseProcessEvent extends BaseModelEvent<ProcessTaskActor> implements ProcessEvent {
    protected BaseProcessEvent(ProcessTaskActor processInstance) {
        super(processInstance);
    }

    protected BaseProcessEvent(ValueMap json) {
        super(json);
    }

    @Override
    public void updateState(ProcessTaskActor actor) {
        // Nothing to update here. (as of now)
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }
}
