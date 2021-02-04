package org.cafienne.processtask.implementation.mail;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;
import org.cafienne.akka.actor.serialization.json.StringValue;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.util.Guid;

import java.net.SocketException;
import java.net.URI;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;

public class CalendarInvite {
    CalendarInvite(ValueMap input) {//throws SocketException {

        Instant startMoment = input.rawInstant("start");
        Instant endMoment = input.rawInstant("end");
        String timeZone = input.has("timeZone") ? input.raw("timeZone") : ZoneId.systemDefault().toString();
        String meetingName = input.raw("meetingName");
        String description = input.has("description") ? input.raw("description") : "";
        String location = input.has("location") ? input.raw("location") : "";

        // Take meeting id from input if defined, or generate a unique identifier..
        String meetingId = input.has("uid") ? input.raw("uid") : new RandomUidGenerator().generateUid().toString();

        // Convert timezone
        try {
            ZoneId.of(timeZone);
        } catch (DateTimeException invalidTimeZone) {

        }
        startMoment.atZone(ZoneId.of(timeZone));
        endMoment.atZone(ZoneId.of(timeZone));

        // Create the event
        String eventName = meetingName;
        DateTime start = new DateTime(Date.from(startMoment));
        DateTime end = new DateTime(Date.from(endMoment));
        VEvent meeting = new VEvent(start, end, eventName);

        meeting.getProperties().add(new Description(description));
        meeting.getProperties().add(new Location(location));


        // add timezone info..
        TzId tz = new TzId(timeZone);
        meeting.getProperties().add(tz);


        meeting.getProperties().add(new Uid(meetingId));


        // add attendees..
        Attendee[] required = getAttendees(input.get("required"));
        for (Attendee attendee : required) {
            attendee.getParameters().add(Role.REQ_PARTICIPANT);
            meeting.getProperties().add(attendee);
        }
        Attendee[] optional = getAttendees(input.get("optional"));
        for (Attendee attendee : optional) {
            attendee.getParameters().add(Role.OPT_PARTICIPANT);
            meeting.getProperties().add(attendee);
        }

        // Create a calendar
        Calendar icsCalendar = new Calendar();
        icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.getProperties().add(CalScale.GREGORIAN);

        // Add the event and print
        icsCalendar.getComponents().add(meeting);
        invite = icsCalendar.toString();
    }

    public final String invite;


    /**
     * Convert the json structure to a list of addresses
     *
     * @param input
     * @return
     */
    private Attendee[] getAttendees(Value<?> input) {
        Collection<Attendee> list = new ArrayList<>();
        if (input.isMap() && !input.asMap().getValue().isEmpty()) {
            list.add(getAttendee(input));
        } else if (input.isList()) {
            input.asList().forEach(value -> list.add(getAttendee(value)));
        } else if (input.getValue() == null) {
            // do nothing, the field is not defined.
        } else if (input.getValue() instanceof String) {
            list.add(getAttendee(input));
        } else {
            // Wrong type of input; ignore it.
        }
        return list.toArray(new Attendee[list.size()]);
    }

    /**
     * Convert the json structure to an address
     *
     * @param value
     * @return
     */
    private Attendee getAttendee(Value value) throws InvalidMailException {
//        System.out.println("Converting value to attendee: " + value);
        String email = "";
        String name = "";

        if (value.isMap()) {
            email = value.asMap().raw("email");
            name = value.asMap().raw("name");
            if (email == null) email = "";
            if (name == null) name = "";
        } else if (value.isPrimitive() && value instanceof StringValue) {
            email = ((StringValue) value).getValue();
        } else {
            throw new InvalidMailAddressException("Cannot extract an email address from an object of type " + value.getClass().getSimpleName());
        }
        if (email == null || email.isBlank()) {
            throw new InvalidMailAddressException("Missing email address in object of type " + value.getClass().getSimpleName());
        }
        Attendee attendee = new Attendee(URI.create("mailto:" + email));
        if (!name.isBlank()) {
            attendee.getParameters().add(new Cn(name));
        }
        return attendee;
    }


    public static void main(String[] args) throws SocketException {
        new CalendarInvite(new ValueMap());
    }
}
