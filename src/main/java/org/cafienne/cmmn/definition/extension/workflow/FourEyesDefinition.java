package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class FourEyesDefinition extends CMMNElementDefinition {
    private final Collection<ItemDefinitionReference> opposites = new ArrayList<>();;

    public FourEyesDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        parse("task", ItemDefinitionReference.class, opposites);
    }

    public Collection<ItemDefinitionReference> getOthers() {
        return opposites;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameFourEyesDefinition);
    }

    private boolean sameFourEyesDefinition(FourEyesDefinition other) {
        return same(this.opposites, other.opposites);
    }
}
