/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.casefile;

import org.cafienne.cmmn.definition.DefinitionsDocument;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of CMMN spec 5.1.4
 */
public class CaseFileItemDefinitionDefinition extends ModelDefinition {

    private final DefinitionType definitionType;
    private final String structureRef;
    private final String importRef;
    private ImportDefinition importDefinition;
    private final Map<String, PropertyDefinition> properties = new LinkedHashMap<>();

    public CaseFileItemDefinitionDefinition(Element definitionElement, DefinitionsDocument document) {
        super(definitionElement, document);
        this.definitionType = readDefinitionType();
        this.structureRef = parseAttribute("structureRef", false, "");
        this.importRef = parseAttribute("importRef", false, "");
        parse("property", PropertyDefinition.class, properties);
    }

    private DefinitionType readDefinitionType() {
        String typeName = parseAttribute("definitionType", false, "http://www.omg.org/spec/CMMN/DefinitionType/Unspecified");
        return DefinitionType.resolveDefinitionType(typeName);
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        if (!importRef.isEmpty()) {
            importDefinition = getModelDefinition().getDefinitionsDocument().getImportDefinition(importRef);
            if (importDefinition == null) {
                super.addReferenceError("The case file item definition '" + this.getName() + "' refers to an import named " + importRef + ", but that definition is not found");
            }
        }
    }

    public DefinitionType getDefinitionType() {
        return definitionType;
    }

    public String getStructureRef() {
        return structureRef;
    }

    public ImportDefinition getImport() {
        return importDefinition;
    }

    public Map<String, PropertyDefinition> getProperties() {
        return properties;
    }

    public Collection<PropertyDefinition> getBusinessIdentifiers() {
        return properties.values().stream().filter(PropertyDefinition::isBusinessIdentifier).collect(Collectors.toList());
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCaseFileItemDefinitionDefinition);
    }

    public boolean sameCaseFileItemDefinitionDefinition(CaseFileItemDefinitionDefinition other) {
        return sameModelDefinition(other)
                && same(definitionType, other.definitionType)
                && same(structureRef, other.structureRef)
                && same(importDefinition, other.importDefinition)
                && same(properties.values(), other.properties.values());
    }
}
