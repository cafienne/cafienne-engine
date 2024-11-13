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

package com.casefabric.cmmn.actorapi.event;

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.actormodel.event.BaseModelEvent;
import com.casefabric.cmmn.instance.Case;
import com.casefabric.json.ValueMap;

import java.io.IOException;

public abstract class CaseBaseEvent extends BaseModelEvent<Case> implements CaseEvent {
    protected CaseBaseEvent(Case caseInstance) {
        super(caseInstance);
    }

    protected CaseBaseEvent(ValueMap json) {
        super(json);
    }

    protected void writeCaseEvent(JsonGenerator generator) throws IOException {
        super.writeModelEvent(generator);
    }

    @Override
    public void updateState(Case actor) {
    }
}
