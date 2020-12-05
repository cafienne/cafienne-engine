package org.cafienne.cmmn.definition.parameter;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.ExpressionDefinition;
import org.w3c.dom.Element;

public class BindingRefinementDefinition extends ExpressionDefinition {

    public BindingRefinementDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

    public BindingOperation getRefinementOperation() {
        String body = getBody() != null ? getBody().trim() : "";
        for (BindingOperation operation : BindingOperation.values()) {
            if (operation.toString().equalsIgnoreCase(body)) {
                return operation;
            }
        }
        return BindingOperation.Replace;
    }
}
