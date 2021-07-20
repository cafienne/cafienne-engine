/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition.casefile;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

/**
 * Implementation of CMMN spec 5.1.4.1
 */
public class PropertyDefinition extends CMMNElementDefinition {
    public enum PropertyType {
        String("http://www.omg.org/spec/CMMN/PropertyType/string"),
        Boolean("http://www.omg.org/spec/CMMN/PropertyType/boolean"),
        Integer("http://www.omg.org/spec/CMMN/PropertyType/integer"),
        Float("http://www.omg.org/spec/CMMN/PropertyType/float"),
        Double("http://www.omg.org/spec/CMMN/PropertyType/double"),
        Duration("http://www.omg.org/spec/CMMN/PropertyType/duration"),
        DateTime("http://www.omg.org/spec/CMMN/PropertyType/dateTime"),
        Time("http://www.omg.org/spec/CMMN/PropertyType/time"),
        Date("http://www.omg.org/spec/CMMN/PropertyType/date"),
        GYearMonth("http://www.omg.org/spec/CMMN/PropertyType/gYearMonth"),
        GYear("http://www.omg.org/spec/CMMN/PropertyType/gYear"),
        GMonthDay("http://www.omg.org/spec/CMMN/PropertyType/gMonthDay"),
        GDay("http://www.omg.org/spec/CMMN/PropertyType/gDay"),
        GMonth("http://www.omg.org/spec/CMMN/PropertyType/gMonth"),
        HexBinary("http://www.omg.org/spec/CMMN/PropertyType/hexBinary"),
        Base64Binary("http://www.omg.org/spec/CMMN/PropertyType/base64Binary"),
        AnyURI("http://www.omg.org/spec/CMMN/PropertyType/anyURI"),
        QName("http://www.omg.org/spec/CMMN/PropertyType/QName"),
        Decimal("http://www.omg.org/spec/CMMN/PropertyType/decimal"),
        Unspecified("http://www.omg.org/spec/CMMN/PropertyType/Unspecified");

        private final String uri;

        PropertyType(String uri) {
            this.uri = uri;
        }

        @Override
        public java.lang.String toString() {
            return uri;
        }

        public static PropertyType getEnum(String value) {
            if (value == null) return null;
            for (PropertyType type : values())
                if (type.toString().equals(value)) return type;

            return null;
        }
    }

    private final PropertyType type;
    private final boolean isBusinessIdentifier;

    public PropertyDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        String typeDescription = parseAttribute("type", false, "");
        type = PropertyType.getEnum(typeDescription);
        if (type == null) {
            getModelDefinition().addDefinitionError(getParentElement().getType() + " " + getParentElement().getName() + " is invalid, because property " + getName() + " has unrecognized type " + typeDescription);
        }
        isBusinessIdentifier = readBusinessIdentifiership();
    }

    private boolean readBusinessIdentifiership() {
        Element cafienneImplementation = getExtension("implementation", false);
        if (cafienneImplementation == null) {
            return false;
        }
        String value = cafienneImplementation.getAttribute("isBusinessIdentifier");
        return value.equalsIgnoreCase("true");
    }

    public boolean isBusinessIdentifier() {
        return isBusinessIdentifier;
    }

    /**
     * Returns the type of property.
     *
     * @return
     */
    public PropertyType getPropertyType() {
        return type;
    }

}
