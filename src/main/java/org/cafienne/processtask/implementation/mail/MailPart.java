package org.cafienne.processtask.implementation.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.instance.ProcessTaskActor;

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
