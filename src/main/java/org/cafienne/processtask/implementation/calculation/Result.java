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

package org.cafienne.processtask.implementation.calculation;

import org.cafienne.json.Value;
import org.cafienne.processtask.implementation.calculation.operation.Source;

public class Result {
    private final Calculation calculation;
    private final Source<?> step;
    private final Value<?> value;

    public Result(Calculation calculation, Source<?> step, Value<?> value) {
        this.calculation = calculation;
        this.step = step;
        this.value = value;
    }

    /**
     * Get the Result value (a clone of the internal value)
     * @param
     * @return
     */
    public Value<?> getValue() {
        return value.cloneValueNode();
    }
}
