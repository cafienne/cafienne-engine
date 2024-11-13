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

package com.casefabric.humantask.actorapi.command;

import com.casefabric.actormodel.identity.CaseUserIdentity;
import com.casefabric.actormodel.response.CommandFailure;
import com.casefabric.cmmn.instance.task.validation.ValidationResponse;
import com.casefabric.humantask.actorapi.response.HumanTaskValidationResponse;
import com.casefabric.humantask.instance.WorkflowTask;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

/**
 * Saves the output in the task. This output is not yet stored back in the case file, since that happens only when the task is completed.
 */
@Manifest
public class ValidateTaskOutput extends TaskOutputCommand {
	public ValidateTaskOutput(CaseUserIdentity user, String caseInstanceId, String taskId, ValueMap taskOutput) {
		super(user, caseInstanceId, taskId, taskOutput);
	}

	public ValidateTaskOutput(ValueMap json) {
		super(json);
	}

	@Override
	public void processTaskCommand(WorkflowTask workflowTask) {
		ValidationResponse response = workflowTask.getTask().validateOutput(taskOutput);
		if (response.isValid()) {
			setResponse(new HumanTaskValidationResponse(this, response.getContent()));
		} else {
			// HTD
//        	return null;
			setResponse(new CommandFailure(this, response.getException()));
		}
	}
}
