package org.cafienne.cmmn.definition.extension.workflow;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.DefinitionElement;
import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class TaskPairingDefinition extends CMMNElementDefinition {
    private final Collection<ItemDefinitionReference> stringReferences = new ArrayList<>();
    protected final Collection<ItemDefinition> directReferences = new ArrayList<>();
    protected final Collection<ItemDefinition> allReferences = new ArrayList<>();
    protected final ItemDefinition parent;

    protected TaskPairingDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        parse("task", ItemDefinitionReference.class, stringReferences);
        this.parent = getParentElement();
    }

    @Override
    public String getContextDescription() {
        return getType() + " in " + getParentElement().getContextDescription();
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        // First make sure that the underlying reference to the ItemDefinition is resolved.
        this.stringReferences.forEach(ItemDefinitionReference::resolveReferences);
        // Now build up the list of direct references. This can be used in the next step to find indirect references.
        this.stringReferences.forEach(reference -> directReferences.add(reference.getItemDefinition()));
        // Now tell our subclasses to load the indirect references as well.
        this.loadIndirectReferences();
    }

    /**
     * Both four eyes and rendez vous have different ways of indirect references.
     * For example, rendez vous has rendez vous with all items that are referenced from the rendez vous of an item that we reference.
     * And four eyes has four eyes also with all items that have rendez vous with an item that we reference
     */
    protected abstract void loadIndirectReferences();

    @Override
    protected void validateElement() {
        super.validateElement();
        this.stringReferences.stream().map(ItemDefinitionReference::getItemDefinition).forEach(reference -> {
            TaskPairingDefinition referenceDefinition = getCounterPart(reference);
            if (referenceDefinition == null) {
                getModelDefinition().addDefinitionError(getContextDescription() + " refers to " + reference.getName() + ", but " + reference.getName() + " does not have " + getType() + " defined");
            } else {
                if (!referenceDefinition.references(getParentElement())) {
                    getModelDefinition().addDefinitionError(getContextDescription() + " refers to " + reference.getName() + ", but " + referenceDefinition.getContextDescription() + " does not refer to " + getParentElement().getContextDescription());
                }
            }
        });
    }

    public String getAllReferredItemNames() {
        return getAllReferences().stream().map(DefinitionElement::getName).collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Returns the 4-eyes definition or the rendez-vous definition of the item definition we're referring to.
     */
    protected abstract TaskPairingDefinition getCounterPart(ItemDefinition reference);

    public boolean hasReferences() {
        return !this.stringReferences.isEmpty();
    }

    public boolean references(ItemDefinition item) {
        return this.getAllReferences().contains(item);
    }

    /**
     * Return both direct and indirect references to this definition.
     */
    public Collection<ItemDefinition> getAllReferences() {
        return allReferences;
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::samePairingDefinition);
    }

    private boolean samePairingDefinition(TaskPairingDefinition other) {
        return same(this.stringReferences, other.stringReferences);
    }
}
