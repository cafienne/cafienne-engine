/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.casefile;

import java.util.Collection;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.cmmn.definition.Multiplicity;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemArray;
import org.cafienne.cmmn.instance.casefile.CaseFileItemCollection;
import org.cafienne.cmmn.instance.casefile.Path;
import org.cafienne.akka.actor.serialization.json.Value;
import org.w3c.dom.Element;

public class CaseFileItemDefinition extends CaseFileItemCollectionDefinition {
    private final Multiplicity multiplicity;
    private final String definitionRef;
    private final String sourceRef;
    private final String targetRefs;
    private CaseFileItemDefinitionDefinition typeDefinition;

    public CaseFileItemDefinition(Element element, Definition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.multiplicity = readMultiplicity();
        this.definitionRef = parseAttribute("definitionRef", true, "");
        this.sourceRef = parseAttribute("sourceRef", false, "");
        this.targetRefs = parseAttribute("targetRefs", false, "");
        parseGrandChildren("children", "caseFileItem", CaseFileItemDefinition.class, getChildren());
    }

    private Multiplicity readMultiplicity() {
        String multiplicityString = parseAttribute("multiplicity", false, "Unspecified");
        try {
            return Multiplicity.valueOf(multiplicityString);
        } catch (IllegalArgumentException iae) {
            getCaseDefinition().addDefinitionError(multiplicityString + " is not a valid multiplicity (in CaseFileItem " + getName() + ")");
            return null;
        }
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        this.typeDefinition = getCaseDefinition().getDefinitionsDocument().getCaseFileItemDefinition(this.definitionRef);
        if (this.typeDefinition == null) {
            getDefinition().addReferenceError("The case file item '" + this.getName() + "' refers to a definition named " + definitionRef + ", but that definition is not found");
            return; // Avoid further checking on this element
        }

        // Resolve source ...
        if (!sourceRef.isEmpty()) {
        }

        // Resolve targets ...
        if (!targetRefs.isEmpty()) {
        }
    }

    public Multiplicity getMultiplicity() {
        return multiplicity;
    }

    public CaseFileItemDefinitionDefinition getCaseFileItemDefinition() {
        return typeDefinition;
    }

    public Collection<CaseFileItemDefinition> getChildren() {
        return getItems();
    }

    public CaseFileItemDefinition getChild(String identifier) {
        return getChildren().stream().filter(i -> i.getName().equals(identifier) || i.getId().equals(identifier)).findFirst().orElse(null);
    }

    /**
     * Recursively searches this level and all children until an item with the specified name is found.
     * 
     * @param identifier
     * @return
     */
    public CaseFileItemDefinition findCaseFileItem(String identifier) {
        CaseFileItemDefinition item = getChild(identifier);
        if (item == null) {
            for (CaseFileItemDefinition caseFileItem : getChildren()) {
                item = caseFileItem.findCaseFileItem(identifier);
                if (item != null) {
                    // Immediately return if we found one.
                    return item;
                }
            }
        }
        return item;
    }

    /**
     * Returns a path to this case file item definition.
     *
     * @return
     */
    public Path getPath() {
        return new Path(this);
    }

    /**
     * Creates a new case file item, based on this definition.
     *
     * @param caseInstance
     * @param parent
     * @return
     */
    public CaseFileItem createInstance(Case caseInstance, CaseFileItemCollection<?> parent) {
        if (multiplicity.isIterable()) {
            return new CaseFileItemArray(caseInstance, this, parent);
        } else {
            return new CaseFileItem(caseInstance, this, parent);
        }
    }

    /**
     * Recursively validates the potential value against this definition;
     * Checks whether the potential value matches the CaseFileItemDefinitionDefinition;
     * and, if there are children in the value, then also matches those children against our children.
     * @param value
     */
    public void validate(Value value) throws CaseFileError {
        getCaseFileItemDefinition().getDefinitionType().validate(this, value);
    }

    /**
     * Returns a collection with the business identifiers of this case file item. Can be empty.
     * @return
     */
    public Collection<PropertyDefinition> getBusinessIdentifiers() {
        return getCaseFileItemDefinition().getBusinessIdentifiers();
    }
}
