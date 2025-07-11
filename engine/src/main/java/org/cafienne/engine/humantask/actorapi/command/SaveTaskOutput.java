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

package org.cafienne.engine.humantask.actorapi.command;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.engine.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Saves the output in the task. This output is not yet stored back in the case file, since that happens only when the task is completed.
 */
@Manifest
public class SaveTaskOutput extends TaskOutputCommand {
	public SaveTaskOutput(CaseUserIdentity user, String caseInstanceId, String taskId, ValueMap taskOutput) {
		super(user, caseInstanceId, taskId, taskOutput);
	}

	public SaveTaskOutput(ValueMap json) {
		super(json);
	}

	@Override
	public void processTaskCommand(WorkflowTask workflowTask) {
		workflowTask.saveOutput(this.taskOutput);
	}
}
