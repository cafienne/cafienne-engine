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

package com.casefabric.cmmn.definition;

import com.casefabric.cmmn.instance.Case;
import com.casefabric.cmmn.instance.EventListener;
import com.casefabric.cmmn.instance.Stage;
import org.w3c.dom.Element;

public abstract class EventListenerDefinition extends PlanItemDefinitionDefinition {
    protected EventListenerDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public abstract EventListener<?> createInstance(String id, int index, ItemDefinition itemDefinition, Stage<?> stage, Case caseInstance);
}
