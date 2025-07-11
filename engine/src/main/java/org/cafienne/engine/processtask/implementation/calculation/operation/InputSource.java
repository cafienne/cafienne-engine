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

package org.cafienne.engine.processtask.implementation.calculation.operation;

import org.cafienne.engine.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.engine.processtask.implementation.calculation.Calculation;
import org.cafienne.engine.processtask.implementation.calculation.Result;
import org.cafienne.engine.processtask.implementation.calculation.definition.source.InputParameterSourceDefinition;

public class InputSource extends Source<InputParameterSourceDefinition> {
    private final InputParameterDefinition parameter;

    public InputSource(InputParameterSourceDefinition inputParameterSourceDefinition, Calculation calculation, InputParameterDefinition parameter) {
        super(inputParameterSourceDefinition, calculation);
        this.parameter = parameter;
    }

    /**
     * Return the outcome of the calculation, if required first calculate it.
     *
     * @return
     */
    protected Result calculateResult() {
        return new Result(calculation, this, calculation.getTask().getMappedInputParameters().get(parameter.getName()));
    }
}
