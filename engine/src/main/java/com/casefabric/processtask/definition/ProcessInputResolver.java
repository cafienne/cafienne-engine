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

package com.casefabric.processtask.definition;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.definition.expression.ResolverDefinition;
import com.casefabric.cmmn.definition.parameter.InputParameterDefinition;
import com.casefabric.cmmn.expression.spel.api.process.InputMappingRoot;
import com.casefabric.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Creates a Resolver on the content of the element, and binds it to the input parameters of the process.
 */
public class ProcessInputResolver extends ResolverDefinition {
    protected ProcessInputResolver(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    public Map<String, InputParameterDefinition> getInputParameters() {
        return getModelDefinition().getInputParameters();
    }

    @SafeVarargs
    public final <T> T resolve(ProcessTaskActor task, T... defaultValue) {
        return resolve(new InputMappingRoot(task), defaultValue);
    }
}
