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

package com.casefabric.processtask.implementation.calculation.definition.source;

import com.casefabric.cmmn.definition.parameter.InputParameterDefinition;
import com.casefabric.processtask.implementation.calculation.Calculation;
import com.casefabric.processtask.implementation.calculation.definition.StepDefinition;
import com.casefabric.processtask.implementation.calculation.operation.InputSource;
import com.casefabric.processtask.implementation.calculation.operation.Source;

public class InputParameterSourceDefinition implements SourceDefinition {
    private final InputParameterDefinition parameter;

    public InputParameterSourceDefinition(InputParameterDefinition parameter) {
        this.parameter = parameter;
    }

    @Override
    public boolean hasDependency(StepDefinition stepDefinition) {
        return false;
    }

    @Override
    public Source<?> createInstance(Calculation calculation) {
        return new InputSource(this, calculation, parameter);
    }

    @Override
    public String getIdentifier() {
        return parameter.getName();
    }

    @Override
    public String getType() {
        return "Input parameter";
    }
}
