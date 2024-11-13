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

package com.casefabric.humantask.actorapi.event;

import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.humantask.instance.TaskAction;
import com.casefabric.humantask.instance.TaskState;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

@Manifest
public class HumanTaskRevoked extends HumanTaskAssigned {

    public HumanTaskRevoked(HumanTask task, String assignee, TaskState nextState, TaskAction transition) {
        super(task, assignee, nextState, transition);
    }

    public HumanTaskRevoked(ValueMap json) {
        super(json);
    }
}
