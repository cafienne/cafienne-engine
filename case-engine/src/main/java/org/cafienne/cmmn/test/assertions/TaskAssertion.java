/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.test.assertions;

import org.cafienne.cmmn.akka.event.plan.PlanItemCreated;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Task;
import org.cafienne.cmmn.instance.task.cmmn.CaseTask;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.cmmn.instance.task.process.ProcessTask;
import org.cafienne.cmmn.test.CaseTestCommand;

public class TaskAssertion extends PlanItemAssertion {

    TaskAssertion(CaseTestCommand command, PlanItemCreated planItem) {
        super(command, planItem);
        super.assertType(new Class[]{Task.class, HumanTask.class, ProcessTask.class, CaseTask.class});
    }

    @Override
    public <T extends PlanItem<?>> TaskAssertion assertType(Class<T> typeClass) {
        return (TaskAssertion) super.assertType(typeClass);
    }
}
