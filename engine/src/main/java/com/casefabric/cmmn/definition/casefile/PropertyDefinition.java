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

package com.casefabric.cmmn.definition.casefile;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
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
        return getImplementationAttribute("isBusinessIdentifier");
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

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::samePropertyDefinition);
    }

    public boolean samePropertyDefinition(PropertyDefinition other) {
        return sameName(other)
                && same(type, other.type)
                && same(isBusinessIdentifier, other.isBusinessIdentifier);
    }
}
