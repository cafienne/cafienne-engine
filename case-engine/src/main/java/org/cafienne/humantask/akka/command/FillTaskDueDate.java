/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.command;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskDueDateFilled;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class FillTaskDueDate extends WorkflowCommand {
	private final Instant dueDate;

	public FillTaskDueDate(TenantUser tenantUser, String caseInstanceId, String taskId, Instant dueDate) {
		super(tenantUser, caseInstanceId, taskId);
		this.dueDate = dueDate;
	}

	public FillTaskDueDate(ValueMap json) {
		super(json);
		this.dueDate = json.rawInstant(Fields.dueDate);
	}

	@Override
	public void validate(HumanTask task) {
		// Only case owners can set the due date
		super.validateCaseOwnership(task);
	}

	@Override
	public HumanTaskResponse process(WorkflowTask workflowTask) {
		workflowTask.setDueDate(dueDate);
		return new HumanTaskResponse(this);
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.write(generator);
		writeField(generator, Fields.dueDate, dueDate);
	}
}