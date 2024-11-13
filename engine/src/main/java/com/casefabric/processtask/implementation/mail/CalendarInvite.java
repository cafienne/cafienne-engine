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

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.util.ByteArrayDataSource;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import com.casefabric.json.Value;
import com.casefabric.json.ValueList;
import com.casefabric.json.ValueMap;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

public class CalendarInvite {
    private final String invite;

    private final Mail mail;
    private final ValueMap input;

    public CalendarInvite(Mail mail, ValueMap input) {
        this.mail = mail;
        this.input = input;

        if (!input.has("required") && !input.has("optional")) {
            input.put("required", input.get("to"));
            input.put("optional", input.get("cc"));
        }

        Instant startMoment = input.rawInstant("start");
        Instant endMoment = input.rawInstant("end");
        String timeZone = input.has("timeZone") ? input.raw("timeZone") : ZoneId.systemDefault().toString();
        String description = input.has("description") ? input.raw("description") : "";
        String location = input.has("location") ? input.raw("location") : "";

        // Take meeting id from input if defined, or generate a unique identifier..
        String meetingId = input.has("uid") ? input.raw("uid") : new RandomUidGenerator().generateUid().toString();

        // Convert timezone
        startMoment.atZone(ZoneId.of(timeZone));
        endMoment.atZone(ZoneId.of(timeZone));

        // Create the event
        DateTime start = new DateTime(Date.from(startMoment));
        DateTime end = new DateTime(Date.from(endMoment));
        VEvent meeting = new VEvent(start, end, getEventName());

        meeting.getProperties().add(new Description(description));
        meeting.getProperties().add(new Location(location));


        // add timezone info..
        TzId tz = new TzId(timeZone);
        meeting.getProperties().add(tz);


        meeting.getProperties().add(new Uid(meetingId));

        // add attendees..
        // If there are no required attendees, we'll use the people in the "to" list of the mail, and similar for the optional attendees take the people in the cc list of the mail.
        getAttendees("required", mail.getToList()).peek(attendee -> attendee.getParameters().add(Role.REQ_PARTICIPANT)).forEach(attendee -> meeting.getProperties().add(attendee));
        getAttendees("optional", mail.getCcList()).peek(attendee -> attendee.getParameters().add(Role.OPT_PARTICIPANT)).forEach(attendee -> meeting.getProperties().add(attendee));

        // Create a calendar
        Calendar icsCalendar = new Calendar();
        icsCalendar.getProperties().add(new ProdId("-//Events Calendar//iCal4j 1.0//EN"));
        icsCalendar.getProperties().add(CalScale.GREGORIAN);

        // Add the event and print
        icsCalendar.getComponents().add(meeting);
        invite = icsCalendar.toString();
    }

    public BodyPart asPart() throws MessagingException {
        BodyPart attachmentPart = new MimeBodyPart();
        String fileName = "invite.ics";
        String mimeType = "text/calendar";
        DataSource source = new ByteArrayDataSource(invite.getBytes(StandardCharsets.UTF_8), mimeType);
        attachmentPart.setDataHandler(new DataHandler(source));
        attachmentPart.setFileName(fileName);
        return attachmentPart;
    }

    private String getEventName() {
        if (input.has("meetingName")) {
            return input.get("meetingName").getValue().toString();
        } else {
            return mail.getSubject();
        }
    }

    private Stream<Attendee> getAttendees(String type, List<MailAddress> alternative) {
        if (input.has(type)) {
            Value<?> dynamicValue = input.get(type);
            ValueList dynamicList = dynamicValue.isList() ? dynamicValue.asList() : dynamicValue == Value.NULL ? new ValueList() : new ValueList(dynamicValue);
            return dynamicList.getValue().stream().map(MailAddress::new).map(MailAddress::asAttendee);
        } else {
            return alternative.stream().map(MailAddress::asAttendee);
        }
    }
}
