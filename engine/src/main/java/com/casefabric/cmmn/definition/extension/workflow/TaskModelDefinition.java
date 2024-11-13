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

package com.casefabric.cmmn.definition.extension.workflow;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ConstraintDefinition;
import com.casefabric.cmmn.definition.HumanTaskDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.definition.expression.ResolverDefinition;
import com.casefabric.cmmn.definition.parameter.InputParameterDefinition;
import com.casefabric.cmmn.expression.spel.api.cmmn.mapping.TaskInputRoot;
import com.casefabric.cmmn.instance.task.humantask.HumanTask;
import com.casefabric.json.JSONParseFailure;
import com.casefabric.json.JSONReader;
import com.casefabric.json.StringValue;
import com.casefabric.json.Value;
import org.w3c.dom.Element;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class TaskModelDefinition extends ResolverDefinition {
    public TaskModelDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public Map<String, InputParameterDefinition> getInputParameters() {
        return ((WorkflowTaskDefinition) getParentElement()).getInputParameters();
    }

    public Value<?> getValue(HumanTask task) {
        String taskModel = resolve(new TaskInputRoot(task));
        try {
            return JSONReader.parse(taskModel);
        } catch (IOException | JSONParseFailure e) {
            return new StringValue(taskModel);
        }
    }
}