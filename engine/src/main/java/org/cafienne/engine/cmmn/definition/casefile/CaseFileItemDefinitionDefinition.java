/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cafienne.engine.cmmn.definition.casefile;

import org.cafienne.engine.cmmn.definition.DefinitionsDocument;
import org.cafienne.engine.cmmn.definition.ModelDefinition;
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
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameCaseFileItemDefinitionDefinition);
    }

    public boolean sameCaseFileItemDefinitionDefinition(CaseFileItemDefinitionDefinition other) {
        return sameModelDefinition(other)
                && sameDefinitionType(other)
                && same(structureRef, other.structureRef)
                && same(importDefinition, other.importDefinition)
                && same(properties.values(), other.properties.values());
    }

    private boolean sameDefinitionType(CaseFileItemDefinitionDefinition other) {
        // DefinitionType is just a plain object / class, and comparison must be done based on class rather than object
        return this.definitionType.getClass().equals(other.definitionType.getClass());
    }
}
