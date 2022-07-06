/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.mail;

import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.cafienne.json.Value;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.implementation.mail.definition.AddressDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class Mail extends SubProcess<MailDefinition> {
    private ValueMap input;
    private List<MailAddress> from;
    private List<MailAddress> replyTo;
    private List<MailAddress> toList;
    private List<MailAddress> ccList;
    private List<MailAddress> bccList;
    private String subject;

    public Mail(ProcessTaskActor processTask, MailDefinition definition) {
        super(processTask, definition);

//        logger.warn("\tSENDING MAIL\t" + processTask.getId() + "\n");
    }

    @Override
    public void reactivate() {
        start(); // Just do the call again.
    }

    private Session mailSession;
    private Transport transport;

    public String getSubject() {
        return subject;
    }

    public List<MailAddress> getFrom() {
        return from;
    }

    public List<MailAddress> getReplyTo() {
        return replyTo;
    }

    public List<MailAddress> getToList() {
        return toList;
    }

    public List<MailAddress> getCcList() {
        return ccList;
    }

    public List<MailAddress> getBccList() {
        return bccList;
    }

    private InternetAddress[] asArray(List<MailAddress> list) {
        return list.stream().map(MailAddress::getAddress).toArray(InternetAddress[]::new);
    }

    private void connectMailServer() throws MessagingException {
        processTaskActor.addDebugInfo(() -> "Connecting to mail server");
        long now = System.currentTimeMillis();

        Properties mailServerProperties = getDefinition().getMailProperties();
        String userName = mailServerProperties.get("authentication.user").toString();
        String password = mailServerProperties.get("authentication.password").toString();
        mailSession = Session.getInstance(mailServerProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userName, password);
            }
        });

        transport = mailSession.getTransport();
        transport.connect();
        long done = System.currentTimeMillis();
//        System.out.println("Connect took " + (done - now) + " milliseconds");
        processTaskActor.addDebugInfo(() -> "Connect to mail server took " + (done - now) + " milliseconds");
    }

    private void disconnectMailServer() throws MessagingException {
        long now = System.currentTimeMillis();
        transport.close();
        long done = System.currentTimeMillis();
        processTaskActor.addDebugInfo(() -> "Disconnecting from mail server took " + (done - now) + " milliseconds");
    }

    @Override
    public void start() {
        input = processTaskActor.getMappedInputParameters();

        // Setup email message and recipients
        try {
            connectMailServer();

            // Read email addresses (can be both statically defined or dynamically taken from input parameters)
            from = resolveAddress(getDefinition().getFrom(), "from");
            replyTo = resolveAddress(getDefinition().getReplyTo(), "replyTo");
            toList = resolveAddressList(getDefinition().getToList(), "to");
            ccList = resolveAddressList(getDefinition().getCcList(), "cc");
            bccList = resolveAddressList(getDefinition().getBccList(), "bcc");
            subject = resolveSubject();


            // Create a mail session and message to fill.
            MimeMessage mailMessage = new MimeMessage(mailSession);

            // First validate the recipient list.
            try {
                mailMessage.setRecipients(Message.RecipientType.TO, asArray(toList));
                mailMessage.setRecipients(Message.RecipientType.CC, asArray(ccList));
                mailMessage.setRecipients(Message.RecipientType.BCC, asArray(bccList));
            } catch (InvalidMailException ime) {
                raiseFault("Failed to set recipients for mail message", ime.getCause());
                return;
            }

            // Validate that the mail has recipients
            if (mailMessage.getAllRecipients() == null || mailMessage.getAllRecipients().length == 0) {
                raiseFault("Mail message has no recipients", new IllegalArgumentException("Mail message has no recipients"));
                return;
            }

            // Fill subject
            processTaskActor.addDebugInfo(() -> "Subject: " + subject);
            mailMessage.setSubject(subject);

            mailMessage.addFrom(asArray(from));
            mailMessage.setReplyTo(asArray(replyTo));

            // Fill body, attachments and invites
            Multipart multipart = new MimeMultipart();

            // Set mail content / body
            multipart.addBodyPart(resolveBody().asPart());

            // Add the attachments if any
            List<Attachment> attachments = resolveAttachments();
            processTaskActor.addDebugInfo(() -> "Adding " + attachments.size() + " attachments");
            attachments.forEach(attachment -> {
                try {
                    multipart.addBodyPart(attachment.getBodyPart());
                } catch (MessagingException e) {
                    throw new InvalidMailException("Cannot add attachment with file name '" + attachment.getFileName() + "'", e);
                }
            });

            // As of now only dynamic invites are supported
            List<CalendarInvite> invites = resolveInvites();
            processTaskActor.addDebugInfo(() -> "Adding " + invites.size() + " calendar invite(s)");
            invites.forEach(invite -> {
                try {
                    multipart.addBodyPart(invite.asPart());
                } catch (MessagingException e) {
                    throw new InvalidMailException("Cannot add the invite attachment", e);
                }
            });

            // Finally, set the multipart content of the mail
            mailMessage.setContent(multipart);

            processTaskActor.addDebugInfo(() -> "Sending message to mail server");
            long now = System.currentTimeMillis();
            Address[] recipients = mailMessage.getAllRecipients();
            transport.sendMessage(mailMessage, recipients);
            long done = System.currentTimeMillis();
//            System.out.println("Completed sending email in " + (done - now) + " milliseconds");

            processTaskActor.addDebugInfo(() -> "Completed sending email in " + (done - now) + " milliseconds");
            disconnectMailServer();
        } catch (AddressException aex) {
            raiseFault("Invalid email address in from and/or replyTo", aex);
            return;
        } catch (MessagingException mex) {
            raiseFault("Failed to generate email message", mex);
            return;
        }

        // Set processTaskActor to completed
        raiseComplete();
    }

    @Override
    public void suspend() {
    }

    @Override
    public void terminate() {
    }

    @Override
    public void resume() {
    }

    private String resolveSubject() {
        if (getDefinition().getSubject() == null) {
            return input.has("subject") ? input.get("subject").getValue().toString() : "";
        } else {
            return getDefinition().getSubject().resolve(processTaskActor);
        }
    }

    private MailPart resolveBody() throws MessagingException {
        MailPart body = new MailPart(processTaskActor, getDefinition());
        processTaskActor.addDebugInfo(() -> "Body: " + body);
        return body;
    }

    private List<Attachment> resolveAttachments() {
        // If there are no attachments specified in the definition, we'll check whether there is an input json array called 'attachments'
        if (getDefinition().getAttachmentList().isEmpty()) {
            // Try to dynamically resolve the attachments based on the "attachments" input parameter.
            return input.withArray("attachments").getValue().stream().filter(value -> {
                if (!value.isMap()) {
                    processTaskActor.addDebugInfo(() -> "Attachment must be a json object with 'content' (base64 coded) and optional 'fileName' and 'mimeType'; found json content of type  " + value.getClass().getSimpleName());
                }
                return value.isMap();
            }).map(Value::asMap).map(map -> new Attachment(map, processTaskActor)).filter(Attachment::hasContent).collect(Collectors.toList());
        } else {
            return getDefinition().getAttachmentList().stream().map(definition -> new Attachment(definition, processTaskActor)).filter(Attachment::hasContent).collect(Collectors.toList());
        }
    }

    private List<CalendarInvite> resolveInvites() {
        ValueMap invite = input.with("invite");
        if (!invite.getValue().isEmpty()) {
            return List.of(new CalendarInvite(this, invite));
        } else {
            return List.of();
        }
    }

    private List<MailAddress> resolveAddress(AddressDefinition address, String fieldType) {
        // If the definition is null, we use an empty list.
        List<AddressDefinition> list = address == null ? List.of() : List.of(address);
        return resolveAddressList(list, fieldType);
    }

    private List<MailAddress> resolveAddressList(List<AddressDefinition> addresses, String fieldType) {
        List<MailAddress> list;
        if (addresses.isEmpty()) {
            // Binding based on dynamic input.
            Value<?> dynamicParameterValue = input.get(fieldType);
            ValueList dynamicList = dynamicParameterValue.isList() ? dynamicParameterValue.asList() : dynamicParameterValue == Value.NULL ? new ValueList() : new ValueList(dynamicParameterValue);
            list = dynamicList.getValue().stream().map(MailAddress::new).collect(Collectors.toList());
        } else {
            // Bind based on the hard coded definition
            list = addresses.stream().map(address -> new MailAddress(address, processTaskActor)).collect(Collectors.toList());
        }
        processTaskActor.addDebugInfo(() -> "Field " + fieldType + ": '" + list.stream().map(MailAddress::getAddress).map(InternetAddress::toString).collect(Collectors.joining("; ")) + "'");
        return list;
    }
}
