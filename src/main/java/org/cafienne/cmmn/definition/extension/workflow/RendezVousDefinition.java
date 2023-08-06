package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class RendezVousDefinition extends TaskPairingDefinition {
    public RendezVousDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    protected RendezVousDefinition getCounterPart(ItemDefinition reference) {
        return reference.getRendezVousDefinition();
    }

    @Override
    protected void loadIndirectReferences() {
        fillChain(this);
    }

    private void fillChain(RendezVousDefinition definition) {
        definition.directReferences.stream()
            .filter(ItemDefinition::hasRendezVous) // All should be having rendez-vous, but still, let's ensure
            .filter(item -> !allReferences.contains(item)) // Avoid endless recursion
            .forEach(item -> { // Add the item, and also the ones it references
                allReferences.add(item);
                fillChain(item.getRendezVousDefinition());
            });
    }
}
