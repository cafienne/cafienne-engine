/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskOutputSaved;
import org.cafienne.humantask.instance.TaskState;

import java.io.IOException;

/**
 * Saves the output in the task. This output is not yet stored back in the case file, since that happens only when the task is completed.
 */
@Manifest
public class SaveTaskOutput extends HumanTaskCommand {
	private final ValueMap taskOutput;

	private enum Fields {
		taskOutput
	}

	public SaveTaskOutput(TenantUser tenantUser, String caseInstanceId, String taskId, ValueMap taskOutput) {
		super(tenantUser, caseInstanceId, taskId);
		this.taskOutput = taskOutput;
	}

	public SaveTaskOutput(ValueMap json) {
		super(json);
		this.taskOutput = readMap(json, Fields.taskOutput);
	}

	public ValueMap getOutput() {
		return taskOutput;
	}

	@Override
	public void validate(HumanTask task) {
		super.validateTaskOwnership(task);
		super.validateState(task, TaskState.Assigned, TaskState.Delegated);
	}

	@Override
	public HumanTaskResponse process(HumanTask task) {
		task.addEvent(new HumanTaskOutputSaved(task, this.taskOutput));
		return new HumanTaskResponse(this);
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.write(generator);
		writeField(generator, Fields.taskOutput, taskOutput);
	}
}
