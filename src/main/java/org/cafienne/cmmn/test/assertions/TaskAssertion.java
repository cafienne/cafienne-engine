/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.actorapi.event.plan.PlanItemCreated;
import org.cafienne.cmmn.instance.PlanItemType;
import org.cafienne.cmmn.test.CaseTestCommand;

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
