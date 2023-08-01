package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class RendezVousDefinition extends TaskPairingDefinition {
    public RendezVousDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }
}
