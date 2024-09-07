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
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * A command that works on the output of the task (whether temporary for saving or validation or for completion)
 */
public abstract class TaskOutputCommand extends WorkflowCommand {
	protected final ValueMap taskOutput;

	protected TaskOutputCommand(CaseUserIdentity user, String caseInstanceId, String taskId, ValueMap taskOutput) {
		super(user, caseInstanceId, taskId);
		this.taskOutput = taskOutput;
	}

	protected TaskOutputCommand(ValueMap json) {
		super(json);
		this.taskOutput = json.readMap(Fields.taskOutput);
	}

	public ValueMap getOutput() {
		return taskOutput;
	}

	@Override
	public void validate(HumanTask task) {
		mustBeActive(task);
		verifyTaskPairRestrictions(task);
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.write(generator);
		writeField(generator, Fields.taskOutput, taskOutput);
	}
}
