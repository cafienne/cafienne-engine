package org.cafienne.processtask.implementation.mail;

import jakarta.mail.internet.InternetAddress;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.property.Attendee;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.StringValue;
import org.cafienne.json.Value;
import org.cafienne.processtask.implementation.mail.definition.AddressDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

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
