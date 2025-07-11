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

package org.cafienne.engine.processtask.implementation.mail;

import org.cafienne.engine.processtask.instance.ProcessTaskActor;
import org.cafienne.json.ValueMap;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;

/**
 * Small custom class to print with toString() the body content (for adding the debug information in Mail class)
 */
public class MailPart {
    private final String body;
    private final String type;

    public MailPart(ProcessTaskActor task, MailDefinition definition) {
        if (definition.getBody() == null) {
            ValueMap input = task.getMappedInputParameters();
            body = input.has("body") ? input.get("body").getValue().toString() : "";
            type = "text/html";
        } else {
            body = definition.getBody().getBody(task);
            type = definition.getBody().getBodyType(task);
        }
    }

    public MimeBodyPart asPart() throws MessagingException {
        MimeBodyPart part = new MimeBodyPart();
        part.setContent(body, type);
        return part;
    }

    @Override
    public String toString() {
        return body;
    }
}
