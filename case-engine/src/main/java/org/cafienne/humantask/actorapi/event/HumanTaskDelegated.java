/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.event;

import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskAction;
import org.cafienne.humantask.instance.TaskState;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

@Manifest
public class HumanTaskDelegated extends HumanTaskAssigned {
    public HumanTaskDelegated(HumanTask task, String assignee) {
        super(task, assignee, TaskState.Delegated, TaskAction.Delegate);
    }

    public HumanTaskDelegated(ValueMap json) {
        super(json);
    }
}
