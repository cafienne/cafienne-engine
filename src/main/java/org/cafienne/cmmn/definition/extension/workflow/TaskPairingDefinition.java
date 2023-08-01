package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public abstract class TaskPairingDefinition extends CMMNElementDefinition {
    private final Collection<ItemDefinitionReference> others = new ArrayList<>();;

    protected TaskPairingDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        parse("task", ItemDefinitionReference.class, others);
    }

    public boolean references(ItemDefinition item) {
        return this.getOthers().stream().anyMatch(reference -> reference.getItemDefinition().equals(item));
    }

    public Collection<ItemDefinitionReference> getOthers() {
        return others;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::samePairingDefinition);
    }

    private boolean samePairingDefinition(TaskPairingDefinition other) {
        return same(this.others, other.others);
    }
}
