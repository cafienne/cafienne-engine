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

package org.cafienne.processtask.implementation.calculation.operation;

import org.cafienne.actormodel.debug.DebugInfoAppender;
import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.Result;
import org.cafienne.processtask.implementation.calculation.definition.source.SourceDefinition;

public abstract class Source<D extends SourceDefinition> {
    private Result result;
    protected final D definition;
    protected final Calculation calculation;

    protected Source(D definition, Calculation calculation) {
        this.definition = definition;
        this.calculation = calculation;
    }

    protected void addDebugInfo(DebugInfoAppender appender) {
        calculation.getTask().getCaseInstance().addDebugInfo(appender);
    }

    public D getDefinition() {
        return definition;
    }

    /**
     * Return the outcome of the calculation step, if required first calculate it.
     * @return
     */
    public final Result getResult() {
        if (this.result == null) {
            this.result = calculateResult();
        }
        return this.result;
    }

    /**
     * Return the outcome of the calculation step.
     * This will be invoked only once, from the getResult method.
     * @return
     */
    protected abstract Result calculateResult();

    public boolean isValid() {
        return true;
    }
}
