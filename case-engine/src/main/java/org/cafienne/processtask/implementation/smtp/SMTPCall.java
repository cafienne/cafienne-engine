/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.smtp;

import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.processtask.implementation.SubProcess;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Properties;

/**
 * Created by dannykruitbosch on 12/07/16.
 */
public class SMTPCall extends SubProcess<SMTPCallDefinition> {
    private final static Logger logger = LoggerFactory.getLogger(SMTPCall.class);

    /**
     * TODO: If mailSession has to be static, the SMTP server properties has to come from a global configuration
     */
    Session mailSession;
    MimeMessage mailMessage;

    /**
     * TODO: Temporary fix - sbt assembly issue - sendMail fails while running from jar as there are some configuration files not getting packaged
     * TODO: This must be fixed
     */
    static {
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");
    }

    public SMTPCall(ProcessTaskActor processTask, SMTPCallDefinition definition) {
        super(processTask, definition);
    }

    @Override
    public void reactivate() {
        start(); // Just do the call again.
    }

    @Override
    public void start() {
        ValueMap processInputParameters = processTaskActor.getMappedInputParameters();

        // Set mail properties based on process definition
        Properties mailServerProperties = new Properties();
        mailServerProperties.put("mail.smtp.port", definition.getSMTPPort());

        // Generate mail to send

        mailSession = Session.getDefaultInstance(mailServerProperties, null);
        mailMessage = new MimeMessage(mailSession);

        // Setup email message and recipients
        try {
            String subject = definition.getMailSubject().resolveParameters(processInputParameters).toString();
            String from = definition.getMailFrom().resolveParameters(processInputParameters).toString();
            String reply = definition.getMailReplyTo().resolveParameters(processInputParameters).toString();

            processTaskActor.addDebugInfo(() -> "ReplyTo (reply): " + reply);

            mailMessage.setFrom(new InternetAddress(from));
            if (reply != null) {
                Address[] replyTo = new Address[]{new InternetAddress(reply)};
                mailMessage.setReplyTo(replyTo);
            }

            // Now fill the recipients.
            for (Recipient recipient : definition.getRecipients()) {
                String email = recipient.getEmail(processInputParameters);
                String name = recipient.getName(processInputParameters);
                Message.RecipientType recipientType = recipient.getRecipientType();
                processTaskActor.addDebugInfo(() -> "Setting " + recipientType + " recipient to " + name + " <" + email + ">");
                try {
                    mailMessage.addRecipient(recipientType, new InternetAddress(email, name));
                } catch (UnsupportedEncodingException ex) {
                    logger.error("Invalid email address " + email + " " + ex.getMessage());
                    raiseFault(new RuntimeException("Failed to set recipients for SMTP call", ex));
                    return;
                } catch (MessagingException mex) {
                    logger.error("Unable to set recipient " + email + " " + mex.getMessage());
                    raiseFault(new RuntimeException("Failed to set recipients for SMTP call", mex));
                    return;
                }
            }

            // Fill subject
            mailMessage.setSubject(subject);

            // Fill body/attachments
            setMailContent(processInputParameters);

        } catch (AddressException aex) {
            raiseFault(new RuntimeException("Invalid email address in from and/or replyTo", aex));
            return;
        } catch (MessagingException mex) {
            raiseFault(new RuntimeException("Failed to generate email message", mex));
            return;
        }

        // Setup connection and send mail
        try {
            Transport transport = mailSession.getTransport("smtp");
            transport.connect(definition.getSMTPServer(), "", "");
            transport.sendMessage(mailMessage, mailMessage.getAllRecipients());
            transport.close();
        } catch (NoSuchProviderException ex) {
            // we should never get here since provider is set hardcoded to "smtp"
            raiseFault(new RuntimeException("No such provider", ex));
            return;
        } catch (MessagingException mex) {
            raiseFault(new RuntimeException("Unable to process and send SMTP message", mex));
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

    private void setMailContent(ValueMap processInputParameters) throws MessagingException {
        Multipart multipart = new MimeMultipart();

        // Set mail content / body
        String body = definition.getMailBody().resolveParameters(processInputParameters).toString();
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(body, definition.getMailBodyType());
        multipart.addBodyPart(messageBodyPart);

        // Add the attachments if any
        for (Attachment attachment : definition.getAttachments()) {
            BodyPart attachmentPart = new MimeBodyPart();
            String content = attachment.getContent().resolveParameters(processInputParameters).toString();
            DataSource source = new ByteArrayDataSource(Base64.getDecoder().decode(content), "application/octet-stream");
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(attachment.getName().resolveParameters(processInputParameters).toString());
            multipart.addBodyPart(attachmentPart);
        }

        mailMessage.setContent(multipart);
    }
}
