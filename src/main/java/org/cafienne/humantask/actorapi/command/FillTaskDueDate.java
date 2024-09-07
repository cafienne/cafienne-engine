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

package org.cafienne.humantask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class FillTaskDueDate extends TaskManagementCommand {
	private final Instant dueDate;

	public FillTaskDueDate(CaseUserIdentity user, String caseInstanceId, String taskId, Instant dueDate) {
		super(user, caseInstanceId, taskId);
		this.dueDate = dueDate;
	}

	public FillTaskDueDate(ValueMap json) {
		super(json);
		this.dueDate = json.rawInstant(Fields.dueDate);
	}

	@Override
	protected void validateTaskAction(HumanTask task) {
		// Nothing to validate here
	}

	@Override
	public void processTaskCommand(WorkflowTask workflowTask) {
		workflowTask.setDueDate(dueDate);
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.write(generator);
		writeField(generator, Fields.dueDate, dueDate);
	}
}