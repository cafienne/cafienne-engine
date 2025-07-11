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

package org.cafienne.engine.cmmn.definition.sentry;

import org.cafienne.engine.cmmn.definition.CMMNElementDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.engine.cmmn.instance.sentry.Criterion;
import org.cafienne.engine.cmmn.instance.sentry.OnPart;
import org.w3c.dom.Element;

public abstract class OnPartDefinition extends CMMNElementDefinition {
    public OnPartDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    public abstract CMMNElementDefinition getSourceDefinition();

    public abstract OnPart<?, ?, ?> createInstance(Criterion<?> criterion);

    public CaseFileItemOnPartDefinition asFile() {
        throw new IllegalArgumentException("Cannot cast a " + getClass().getSimpleName() +" to a CaseFileItemOnPartDefinition");
    }

    public PlanItemOnPartDefinition asPlan() {
        throw new IllegalArgumentException("Cannot cast a " + getClass().getSimpleName() +" to a PlanItemOnPartDefinition");
    }
}