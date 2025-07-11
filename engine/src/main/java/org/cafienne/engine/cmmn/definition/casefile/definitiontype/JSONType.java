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

package org.cafienne.engine.cmmn.definition.casefile.definitiontype;

import org.cafienne.engine.cmmn.definition.casefile.CaseFileError;
import org.cafienne.engine.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.engine.cmmn.definition.casefile.DefinitionType;
import org.cafienne.engine.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.util.Map;

public class JSONType extends DefinitionType {

    @Override
    public void validate(CaseFileItemDefinition itemDefinition, Value<?> value) throws CaseFileError {
        if (value.isMap()) {
            validateItem(value.asMap(), itemDefinition);
        } else if (value.isList() && itemDefinition.getMultiplicity().isIterable()) {
            value.asList().getValue().forEach(element -> {
                if (element.isMap()) validateItem(element.asMap(), itemDefinition);
            });
        }
    }

    private void validateItem(ValueMap value, CaseFileItemDefinition itemDefinition) {
        Map<String, PropertyDefinition> properties = itemDefinition.getCaseFileItemDefinition().getProperties();
        // Now iterate the object fields and validate each item.
        value.asMap().getValue().forEach((fieldName, fieldValue) -> {
            // First check to see if it matches one of the properties,
            // and if not, go check for a child item.
            // If also no child item found, then it is accepted as "blob" content
            PropertyDefinition propertyDefinition = properties.get(fieldName);
            if (propertyDefinition != null) {
                validateProperty(propertyDefinition, fieldValue); // Validation may throw TransitionDeniedException
            } else {
                CaseFileItemDefinition childDefinition = itemDefinition.getChild(fieldName);
                if (childDefinition != null) {
                    childDefinition.validatePropertyTypes(fieldValue);
                }
            }
        });
    }

    private void validateProperty(PropertyDefinition propertyDefinition, Value<?> propertyValue) {
        if (propertyValue == null || propertyValue == Value.NULL) { // Null-valued properties match any type, let's just continue.
            return;
        }
        PropertyDefinition.PropertyType type = propertyDefinition.getPropertyType();
        try {
            if (!propertyValue.matches(type)) {
                throw new CaseFileError("Property '" + propertyDefinition.getName() + "' has wrong type, expecting " + type + ", found a " + propertyValue.getClass().getSimpleName());
            }
        } catch (IllegalArgumentException improperType) {
            throw new CaseFileError("Property '" + propertyDefinition.getName() + "' has wrong type, expecting " + type + ", found exception " + improperType.getMessage());
        }
    }
}
