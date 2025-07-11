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
import org.cafienne.engine.cmmn.definition.ConstraintDefinition;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
import org.cafienne.engine.cmmn.instance.sentry.Criterion;
import org.w3c.dom.Element;

public class IfPartDefinition extends ConstraintDefinition {
    static final String TAG_NAME = "ifPart";

    public IfPartDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    IfPartDefinition(ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(modelDefinition, parentElement, TAG_NAME, true); // Default ifPart: evaluates always to true
    }

    public boolean evaluate(Criterion<?> criterion) {
        return getExpressionDefinition().getEvaluator().evaluateIfPart(criterion, this);
    }
}
