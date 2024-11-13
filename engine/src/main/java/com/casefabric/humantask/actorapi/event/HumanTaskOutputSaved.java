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

import com.fasterxml.jackson.core.JsonGenerator;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.infrastructure.serialization.Manifest;
import com.casefabric.json.ValueMap;

import java.io.IOException;

@Manifest
public class HumanTaskOutputSaved extends HumanTaskEvent {
	private final ValueMap taskOutput; // taskOutput - task saved output

	public HumanTaskOutputSaved(HumanTask task, ValueMap output) {
		super(task);
		this.taskOutput = output;
	}

	public HumanTaskOutputSaved(ValueMap json) {
		super(json);
		this.taskOutput = json.readMap(Fields.taskOutput);
	}

	public ValueMap getTaskOutput() {
		return taskOutput;
	}

	@Override
	public String toString() {
		return "HumanTask[" + getTaskId() + "] - Saved output - " + taskOutput;
	}

	@Override
	public void write(JsonGenerator generator) throws IOException {
		super.writeHumanTaskEvent(generator);
		writeField(generator, Fields.taskOutput, taskOutput);
	}
}
