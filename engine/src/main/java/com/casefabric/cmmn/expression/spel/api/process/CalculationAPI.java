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

package org.cafienne.cmmn.expression.spel.api.process;

import org.cafienne.cmmn.expression.spel.api.CaseRootObject;
import org.cafienne.json.Value;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.source.InputReference;
import org.cafienne.processtask.implementation.calculation.operation.CalculationStep;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides context for steps in calculation tasks
 */
public class CalculationAPI extends CaseRootObject {
    private final CalculationStep step;

    public CalculationAPI(Calculation calculation, CalculationStep step, Map<InputReference, Value<?>> sources) {
        super(calculation.getTask().getCaseInstance());
        this.step = step;

        // Make sure we can directly access the task or stage or milestone; e.g. "task.index < 3"
        registerPlanItem(calculation.getTask());

        // Add a reader for each incoming source
        sources.forEach((input, value) -> addPropertyReader(input.getElementName(), () -> value));
    }

    @Override
    public String getDescription() {
        String from = step.getDefinition().getInputs().stream().map(InputReference::getSourceReference).collect(Collectors.joining("', '"));
        String fromSourcesString = from.isEmpty() ? "" : " from '" + from + "'";
        return "calculation step to produce '" + step.getDefinition().getIdentifier() + "'" + fromSourcesString + ". Expression";
    }
}
