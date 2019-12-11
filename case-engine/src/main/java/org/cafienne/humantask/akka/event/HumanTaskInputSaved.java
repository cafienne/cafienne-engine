/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.humantask.akka.event;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.task.humantask.HumanTask;
import org.cafienne.humantask.instance.WorkflowTask;

import java.io.IOException;

@Manifest
public class HumanTaskInputSaved extends HumanTaskEvent {
	private final ValueMap input;

	public enum Fields {
		input
	}

	public HumanTaskInputSaved(HumanTask task, ValueMap input) {
		super(task);
		this.input = input;
	}

	public HumanTaskInputSaved(ValueMap json) {
		super(json);
		this.input = readMap(json, Fields.input);
	}

	public void updateState(WorkflowTask task) {
		task.setInput(input);
	}

	public ValueMap getInput() {
		return input;
	}

	@Override
	public String toString() {
		return "HumanTask[" + getTaskId() + "] - Set input - " + input;
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.writeHumanTaskEvent(generator);
		writeField(generator, Fields.input, input);
	}

	@Override
	protected void recoverHumanTaskEvent(WorkflowTask task) {
		updateState(task);
	}
}
