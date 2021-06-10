package org.cafienne.cmmn.definition.casefile.definitiontype;

import java.util.Map;

import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.casefile.DefinitionType;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.json.ValueMap;
import org.cafienne.json.Value;

public class JSONType extends DefinitionType {

    @Override
    public void validate(CaseFileItemDefinition itemDefinition, Value value) throws CaseFileError {
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

    private void validateProperty(PropertyDefinition propertyDefinition, Value propertyValue) {
        if (propertyValue == null || propertyValue == Value.NULL) { // Null-valued properties match any type, let's just continue.
            return;
        }
        PropertyDefinition.PropertyType type = propertyDefinition.getPropertyType();
        try {
            if (!propertyValue.matches(type)) {
                throw new CaseFileError("Property '" + propertyDefinition.getName() + "' has wrong type, expecting " + type);
            }
        } catch (IllegalArgumentException improperType) {
            throw new CaseFileError("Property '" + propertyDefinition.getName() + "' has wrong type, expecting " + type + ", found exception " + improperType.getMessage());
        }
    }
}
