/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.calculation;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.util.*;

public class CalculationDefinition extends SubProcessDefinition {
    private final Collection<MappingDefinition> mappings = new ArrayList();
    private final Map<String, SourceDefinition> sources = new HashMap();
    private final Map<String, MappingDefinition> targets = new HashMap();

    public CalculationDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        parse("mapping", MappingDefinition.class, mappings);

        // Now, build the mapping chain
        getProcessDefinition().getInputParameters().forEach((name, parameter) -> sources.put(name, new InputParameterSourceDefinition(parameter)));
        mappings.forEach(mapping -> sources.put(mapping.getTarget(), new MappingSourceDefinition(mapping)));

        // Make sure we have all output parameters covered for
        mappings.forEach(mapping -> targets.put(mapping.getTarget(), mapping));
        getProcessDefinition().getOutputParameters().forEach((name, parameter) -> {
          if (!targets.containsKey(name)) {
              processDefinition.addDefinitionError("Calculation Task '"+processDefinition.getName()+"' has an output parameter '" + name +"' but no mapping to fill it.");
          }
        });

        // Now make sure that there are no recursive dependencies in the calculation
        mappings.forEach(mapping -> mapping.checkDependencies(this));
    }

    SourceDefinition getSource(String identifier) {
        return sources.get(identifier);
    }

    MappingDefinition getTarget(String identifier) {
        return targets.get(identifier);
    }

    @Override
    public Calculation createInstance(ProcessTaskActor processTaskActor) {
        return new Calculation(processTaskActor, this);
    }

    @Override
    protected Set<String> getRawOutputParameterNames() {
        return super.getExceptionParameterNames();
    }
}
