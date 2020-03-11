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
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.akka.command.response.HumanTaskResponse;
import org.cafienne.humantask.akka.event.HumanTaskDueDateFilled;

import java.io.IOException;
import java.time.Instant;

@Manifest
public class FillTaskDueDate extends HumanTaskCommand {
	private final Instant dueDate;

	private enum Fields {
		dueDate
	}

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
         // TODO: 1. Validate whether the current user has the privilege to set the due date
	}

	@Override
	public HumanTaskResponse process(HumanTask task) {
		task.addEvent(new HumanTaskDueDateFilled(task, this.dueDate));
		return new HumanTaskResponse(this);
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.write(generator);
		writeField(generator, Fields.dueDate, dueDate);
	}
}