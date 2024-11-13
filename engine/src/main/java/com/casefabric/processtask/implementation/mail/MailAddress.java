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

package com.casefabric.processtask.implementation.mail;

import jakarta.mail.internet.InternetAddress;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.property.Attendee;
import com.casefabric.infrastructure.serialization.Fields;
import com.casefabric.json.StringValue;
import com.casefabric.json.Value;
import com.casefabric.processtask.implementation.mail.definition.AddressDefinition;
import com.casefabric.processtask.instance.ProcessTaskActor;

import java.io.UnsupportedEncodingException;
import java.net.URI;

public class MailAddress {
    private final String email;
    private final String name;

    public MailAddress(Value<?> value) {
//        System.out.println("Reading dynamic address from value " + value);
        if (value.isMap()) {
            email = value.asMap().readString(Fields.email, "");
            name = value.asMap().readString(Fields.name, "");
        } else if (value instanceof StringValue) {
            email = (value).getValue().toString();
            name = "";
        } else {
            throw new InvalidMailAddressException("Cannot extract an email address from an object of type " + value.getClass().getSimpleName());
        }
        if (email == null || email.isBlank()) {
            throw new InvalidMailAddressException("Missing email address in object of type " + value.getClass().getSimpleName());
        }
    }

    public MailAddress(AddressDefinition definition, ProcessTaskActor task) {
        email = definition.getEmailResolver().getValue(task, "");
        name = definition.getNameResolver().getValue(task, "");
    }

    public InternetAddress getAddress() {
        try {
            return new InternetAddress(email, name);
        } catch (UnsupportedEncodingException ex) {
            throw new InvalidMailAddressException("Invalid email address " + email + " " + ex.getMessage(), ex);
        }
    }

    public Attendee asAttendee() {
        Attendee attendee = new Attendee(URI.create("mailto:" + email));
        if (!name.isBlank()) {
            attendee.getParameters().add(new Cn(name));
        }
        return attendee;
    }
}
