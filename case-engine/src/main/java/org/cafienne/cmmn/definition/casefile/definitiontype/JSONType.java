package org.cafienne.cmmn.definition.casefile.definitiontype;

import java.util.Map;

import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.casefile.DefinitionType;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.akka.actor.serialization.json.Value;

public class JSONType extends DefinitionType {

    @Override
    public void validate(CaseFileItemDefinition itemDefinition, Value value) throws CaseFileError {
        if (value.isMap()) {
            final ValueMap object = (ValueMap) value;
            Map<String, PropertyDefinition> properties = itemDefinition.getCaseFileItemDefinition().getProperties();
            if (properties.isEmpty()) {
                // Simply allow to dump the contents and don't do any further validation.
                return;
            }

            // Now iterate the object fields and validate each item.
            object.getValue().forEach((fieldName, fieldValue) -> {
                
                // First check to see if it matches one of the properties,
                // and if not, go check for a child item.
                PropertyDefinition propertyDefinition = properties.get(fieldName);
                if (propertyDefinition != null) {
                    validateProperty(propertyDefinition, fieldValue); // Validation may throw TransitionDeniedException
                } else {
                    CaseFileItemDefinition childDefinition = itemDefinition.getChild(fieldName);
                    if (childDefinition == null) {
                        throw new CaseFileError("Property '" + fieldName + "' is not found in the definition of "+itemDefinition.getName());
                    }
                    childDefinition.validate(fieldValue);
                }
            });
        }
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
