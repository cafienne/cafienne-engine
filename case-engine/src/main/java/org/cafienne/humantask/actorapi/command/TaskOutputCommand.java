/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.actorapi.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Saves the output in the task. This output is not yet stored back in the case file, since that happens only when the task is completed.
 */
@Manifest
public abstract class TaskOutputCommand extends WorkflowCommand {
	protected final ValueMap taskOutput;

	protected TaskOutputCommand(TenantUser tenantUser, String caseInstanceId, String taskId, ValueMap taskOutput) {
		super(tenantUser, caseInstanceId, taskId);
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
		super.validateTaskOwnership(task);
		super.mustBeActive(task);
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.write(generator);
		writeField(generator, Fields.taskOutput, taskOutput);
	}
}
