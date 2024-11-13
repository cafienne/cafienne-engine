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

package com.casefabric.cmmn.test.assertions;

import com.casefabric.cmmn.actorapi.event.plan.PlanItemCreated;
import com.casefabric.cmmn.instance.PlanItemType;
import com.casefabric.cmmn.test.CaseTestCommand;

public class TaskAssertion extends PlanItemAssertion {

    TaskAssertion(CaseTestCommand command, PlanItemCreated planItem) {
        super(command, planItem);
        super.assertType(PlanItemType.HumanTask, PlanItemType.ProcessTask, PlanItemType.CaseTask);
    }

    @Override
    public TaskAssertion assertType(PlanItemType expectedType) {
        return (TaskAssertion) super.assertType(expectedType);
    }
}
