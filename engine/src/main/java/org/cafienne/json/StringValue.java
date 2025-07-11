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

package org.cafienne.json;

import com.fasterxml.jackson.core.JsonGenerator;
import jakarta.xml.bind.DatatypeConverter;
import org.cafienne.engine.cmmn.definition.casefile.PropertyDefinition;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.NamespaceContext;
import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

public class StringValue extends PrimitiveValue<String> {
    public StringValue(String value) {
        super(value);
    }

    @Override
    public void print(JsonGenerator generator) throws IOException {
        generator.writeString(value);
    }

    @Override
    public StringValue cloneValueNode() {
        return new StringValue(value);
    }

    @Override
    public boolean matches(PropertyDefinition.PropertyType propertyType) throws IllegalArgumentException {
        switch (propertyType) {
        case String:
        case Unspecified: // You can put a string in Unspecified
            return true;
        case Date:
            return matchDate();
        case Duration:
            return matchDuration();
        case DateTime:
            return matchDateTime();
        case Time:
            return matchTime();
        case GDay:
            return matchGDay();
        case GMonth:
            return matchGMonth();
        case GMonthDay:
            return matchGMonthDay();
        case GYear:
            return matchGYear();
        case GYearMonth:
            return matchGYearMonth();
        case QName:
            return matchQName();
        case AnyURI:
            return matchAnyURI();
        // All below types must have been recognized already by the JSON parser
        // and have been converted to the corresponding Value object, hence should have
        // never end up in a StringValue
        case Base64Binary:
        case Boolean:
        case Decimal:
        case Double:
        case Float:
        case HexBinary:
        case Integer:
        default:
            return false;
        }
    }

    private boolean matchDateTime() {
        // TODO: figure out a means to conserve the conversion, i.e., avoid doing this conversion twice.
        // For that we'd probably have to introduce a DateTimeValue extending StringValue, and we'd also
        // have to replace this value in our parent (which is probably a ValueMap or a ValueList)
        DatatypeConverter.parseDateTime(value);
        return true;
    }

    private boolean matchDate() {
        DatatypeConverter.parseDate(value);
        return true;
    }

    private boolean matchDuration() {
        getDatatypeFactory().newDuration(value);
        return true;
    }

    private boolean matchTime() {
        DatatypeConverter.parseTime(value);
        return true;
    }

    private boolean matchGDay() {
        int day = getCalendar().getDay();
        return day != Integer.MIN_VALUE;
    }

    private boolean matchGMonthDay() {
        XMLGregorianCalendar calendar = getCalendar();
        int month = calendar.getMonth();
        int day = calendar.getDay();
        return month != Integer.MIN_VALUE && day != Integer.MIN_VALUE;
    }

    private boolean matchGMonth() {
        XMLGregorianCalendar calendar = getCalendar();
        int month = calendar.getMonth();
        return month != Integer.MIN_VALUE;
    }

    private boolean matchGYear() {
        int year = getCalendar().getYear();
        return year != Integer.MIN_VALUE;
    }

    private boolean matchGYearMonth() {
        XMLGregorianCalendar calendar = getCalendar();
        int month = calendar.getMonth();
        int year = calendar.getYear();
        return month != Integer.MIN_VALUE && year != Integer.MIN_VALUE;
    }

    private boolean matchQName() {
        // Currently no namespace support. We could take it from the definition,
        // or also from the current XML document that is being parsed (if at all).
        // But for now, we just don't do anything, let's first await a proper use case.
        DatatypeConverter.parseQName(value, new NamespaceContext() {
            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                // TODO Auto-generated method stub
                return new Vector().iterator();
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return "";
            }

            @Override
            public String getNamespaceURI(String prefix) {
                return "";
            }
        });
        return true;
    }

    private boolean matchAnyURI() {
        // Anything goes (for now)
        return true;
    }

    private XMLGregorianCalendar getCalendar() {
        return getDatatypeFactory().newXMLGregorianCalendar(value);
    }

    private static DatatypeFactory instance;

    private static DatatypeFactory getDatatypeFactory() {
        if (instance == null) {
            try {
                instance = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
                throw new IllegalArgumentException("Cannot find a proper data type factory instance", e);
            }
        }
        return instance;
    }
}