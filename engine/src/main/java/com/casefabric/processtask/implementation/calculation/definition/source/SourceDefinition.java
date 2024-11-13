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

package org.cafienne.processtask.implementation.calculation.definition.source;

import org.cafienne.processtask.implementation.calculation.Calculation;
import org.cafienne.processtask.implementation.calculation.definition.StepDefinition;
import org.cafienne.processtask.implementation.calculation.operation.Source;

import java.util.Collection;
import java.util.Collections;

public interface SourceDefinition {
    /**
     * Determine whether the source is dependent on the mapping
     *
     * @param stepDefinition
     * @return
     */
    boolean hasDependency(StepDefinition stepDefinition);

    Source<?> createInstance(Calculation calculation);

    String getIdentifier();

    default String getType() {
        return this.getClass().getSimpleName();
    }

    default String getDescription() {
        return this.getType() + " '" + getIdentifier() + "'";
    }

    /**
     * Return sources that this source is dependent on.
     *
     * @return
     */
    default Collection<InputReference> getInputs() {
        return Collections.emptySet();
    }
}
