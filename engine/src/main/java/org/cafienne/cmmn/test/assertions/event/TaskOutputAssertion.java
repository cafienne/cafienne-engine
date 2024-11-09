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

package org.cafienne.cmmn.test.assertions.event;

import org.cafienne.cmmn.actorapi.event.plan.task.TaskOutputFilled;
import org.cafienne.json.Value;

/**
 * Assertions around a TaskOutputFilled event
 */
public class TaskOutputAssertion extends CaseInstanceEventAssertion<TaskOutputFilled> {
    public TaskOutputAssertion(TaskOutputFilled event) {
        super(event);
    }

    /**
     * Asserts the the output parameter with the specified name has the expected value.
     * Expected value can be null, or a raw value (e.g., a plain string or number), or a Value object.
     * @param outputParameterName
     * @param expectedValue
     * @return the assertion
     */
    public TaskOutputAssertion assertValue(String outputParameterName, Object expectedValue) {
        Value<?> value = getValue(outputParameterName);
        if (expectedValue == null) {
            if (value!=Value.NULL) {
                throw new AssertionError("Expected field " + outputParameterName + " to be null, but it is not; it is " + expectedValue);
            }
        } else if (!value.getValue().equals(expectedValue)) {
            throw new AssertionError("Task output parameter "+outputParameterName+" does not contain the expected value, but " + value);
        }
        return this;
    }

    /**
     * Get the value of the output parameter with the specified name.
     * @param outputParameterName
     * @param <T>
     * @return
     */
    public <T extends Value<?>> T getValue(String outputParameterName) {
        return (T) event.getTaskOutputParameters().get(outputParameterName);
    }
}
