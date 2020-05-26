/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;

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
        javax.xml.bind.DatatypeConverter.parseDateTime(value);
        return true;
    }

    private boolean matchDate() {
        javax.xml.bind.DatatypeConverter.parseDate(value);
        return true;
    }

    private boolean matchDuration() {
        getDatatypeFactory().newDuration(value);
        return true;
    }

    private boolean matchTime() {
        javax.xml.bind.DatatypeConverter.parseTime(value);
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
        javax.xml.bind.DatatypeConverter.parseQName(value, new NamespaceContext() {
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