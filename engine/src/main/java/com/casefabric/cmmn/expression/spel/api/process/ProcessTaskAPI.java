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

package com.casefabric.cmmn.expression.spel.api.process;

import com.casefabric.cmmn.expression.spel.api.APIObject;
import com.casefabric.processtask.instance.ProcessTaskActor;

public class ProcessTaskAPI extends APIObject<ProcessTaskActor> {
    public ProcessTaskAPI(ProcessTaskActor actor) {
        super(actor);
        addPropertyReader("id", actor::getId);
        addPropertyReader("name", actor::getName);
        addPropertyReader("tenant", actor::getTenant);
        addPropertyReader("parent", actor::getParentActorId);
        addPropertyReader("root", actor::getRootActorId);
    }
}
