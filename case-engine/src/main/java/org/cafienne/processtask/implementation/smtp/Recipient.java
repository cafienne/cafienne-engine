package org.cafienne.processtask.implementation.smtp;

import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.util.StringTemplate;

import javax.mail.Message;

class Recipient {
    private final String email;
    private final String name;
    private final Message.RecipientType recipientType;

    public Recipient(String email, String name, Message.RecipientType recipientType) {
        this.email = email;
        this.name = name;
        this.recipientType = recipientType;
    }

    public String getEmail(ValueMap processInputParameters) {
        StringTemplate nameTemplate = new StringTemplate(email);
        nameTemplate.resolveParameters(processInputParameters);
        return nameTemplate.toString();
    }

    public String getName(ValueMap processInputParameters) {
        StringTemplate valueTemplate = new StringTemplate(name);
        valueTemplate.resolveParameters(processInputParameters);
        return valueTemplate.toString();
    }

    public Message.RecipientType getRecipientType() {
        return this.recipientType;
    }
}