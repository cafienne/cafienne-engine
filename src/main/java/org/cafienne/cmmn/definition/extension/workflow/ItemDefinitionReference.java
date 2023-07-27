package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class ItemDefinitionReference extends CMMNElementDefinition {
    private final String itemRef;
    private final String itemName;
    private ItemDefinition itemDefinition;

    public ItemDefinitionReference(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.itemRef = parseAttribute("taskRef", true);
        this.itemName = parseAttribute("taskName", false, "");
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        this.itemDefinition = getCaseDefinition().getElement(itemRef);
        if (this.itemDefinition == null) {
            getCaseDefinition().addReferenceError("Cannot find a task with reference " + itemRef +" and name '" + itemName + "'");
        }
    }

    public ItemDefinition getItemDefinition() {
        return itemDefinition;
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameItemReference);
    }

    private boolean sameItemReference(ItemDefinitionReference other) {
        return same(this.itemRef, other.itemRef) && same(this.itemName, other.itemName);
    }
}
