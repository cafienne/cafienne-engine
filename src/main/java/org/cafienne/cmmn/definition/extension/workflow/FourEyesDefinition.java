package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.List;
import java.util.stream.Collectors;

public class FourEyesDefinition extends TaskPairingDefinition {
    public FourEyesDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    @Override
    protected FourEyesDefinition getCounterPart(ItemDefinition reference) {
        return reference.getFourEyesDefinition();
    }

    @Override
    protected void loadIndirectReferences() {
        directReferences.stream()
                .filter(ItemDefinition::hasFourEyes) // All should be having rendez-vous, but still, let's ensure
                .filter(item -> !allReferences.contains(item)) // Avoid endless recursion
                .forEach(item -> { // Add the item, and also the ones it references
                    allReferences.add(item);
                    // If one of our counterparts has rendez-vous, then we have also four eyes with all those elements the counterpart has rendez-vous with
                    if (item.hasRendezVous()) {
                        List<ItemDefinition> rendezVousItems = item.getRendezVousDefinition().getAllReferences().stream().filter(element -> !allReferences.contains(element)).collect(Collectors.toList());
                        allReferences.addAll(rendezVousItems);
                    }
                });
    }
}
