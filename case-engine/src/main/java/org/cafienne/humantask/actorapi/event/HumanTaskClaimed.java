/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.event;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.TaskAction;
import org.cafienne.humantask.instance.TaskState;

@Manifest
public class HumanTaskClaimed extends HumanTaskAssigned {
    public HumanTaskClaimed(HumanTask task, String assignee) {
        super(task, assignee, TaskState.Assigned, TaskAction.Claim);
    }

    public HumanTaskClaimed(ValueMap json) {
        super(json);
    }
}
