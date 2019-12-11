package org.cafienne.cmmn.definition.casefile.definitiontype;

import java.util.Map;

import org.cafienne.cmmn.definition.casefile.DefinitionType;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.TransitionDeniedException;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.casefile.Value;

public class JSONType extends DefinitionType {

    @Override
    public void validate(CaseFileItem item, Value<?> value) {
        if (value instanceof ValueMap) {
            final ValueMap object = (ValueMap) value;
            Map<String, PropertyDefinition> properties = item.getDefinition().getCaseFileItemDefinition().getProperties();

            // Now iterate the object fields and validate each item.
            object.getValue().forEach((fieldName, fieldValue) -> {
                
                // First check to see if it matches one of the properties,
                // and if not, go check for a child item.
                PropertyDefinition propertyDefinition = properties.get(fieldName);
                if (propertyDefinition != null) {
                    validateProperty(propertyDefinition, fieldValue, fieldName); // Validation may throw TransitionDeniedException
                } else if (item.getChildDefinition(fieldName) != null) {
                    CaseFileItem childItem = item.getItem(fieldName);
                    childItem.getDefinition().getCaseFileItemDefinition().getDefinitionType().validate(childItem, fieldValue);
                } else { // Couldn't find the property.
                    throw new TransitionDeniedException("Property '" + fieldName + "' is not found in the definition of "+item.getDefinition().getName());
                }
            });
        }
    }

    private void validateProperty(PropertyDefinition propertyDefinition, Value<?> propertyValue, String propertyName) {
        if (propertyValue == null) { // Null-valued properties matches any type, let's just continue.
            return;
        }
        PropertyDefinition.PropertyType type = propertyDefinition.getPropertyType();
        try {
            if (!propertyValue.matches(type)) {
                throw new TransitionDeniedException("Property '" + propertyDefinition.getName() + "' has wrong type, expecting " + type);
            }
        } catch (IllegalArgumentException improperType) {
            throw new TransitionDeniedException("Property '" + propertyDefinition.getName() + "' has wrong type, expecting " + type + ", found exception " + improperType.getMessage());
        }
    }
}
