/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.smtp;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.Definition;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.util.StringTemplate;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 */
public class SMTPCallDefinition extends SubProcessDefinition {
    private final String smtpServer;
    private final String smtpPort;
    private final String subject;
    private final String bodyTemplate;
    private final String sender;
    private final String replyTo;
    private final List<Recipient> recipients = new ArrayList<>();
    private final List<Attachment> attachments = new ArrayList<>();
    private final String bodyType;

    public SMTPCallDefinition(Element element, Definition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.smtpServer = parse("smtp-server", String.class, true);
        this.smtpPort = parse("smtp-port", String.class, true);
        this.subject = parse("subject", String.class, true);

        // Get the mail body
        Element body = XMLHelper.getElement(element, "mail-body");
        this.bodyTemplate = XMLHelper.getContent(body, null, "");
        this.bodyType = body.getAttribute("type");

        this.sender = parse("from", String.class, true);
        this.replyTo = parse("reply-to", String.class, false);

        // Get the recipients
        Element toElement = XMLHelper.getElement(element, "to");
        Element ccElement = XMLHelper.getElement(element, "cc");
        Element bccElement = XMLHelper.getElement(element, "bcc");
        if (toElement != null) {
            addRecipientList(toElement, Message.RecipientType.TO);
        }
        if (ccElement != null) {
            addRecipientList(ccElement, Message.RecipientType.CC);
        }
        if (bccElement != null) {
            addRecipientList(bccElement, Message.RecipientType.BCC);
        }

        addAttachments(element);
    }

    @Override
    public Set<String> getRawOutputParameterNames() {
        // Only exception parameters...
        return super.getExceptionParameterNames();
    }

    String getSMTPServer() {
        return this.smtpServer;
    }

    String getSMTPPort() {
        return this.smtpPort;
    }

    StringTemplate getMailFrom() {
        return new StringTemplate(sender);
    }

    StringTemplate getMailReplyTo() {
        return new StringTemplate(replyTo);
    }

    List<Recipient> getRecipients() {
        return recipients;
    }

    StringTemplate getMailSubject() {
        return new StringTemplate(subject);
    }

    StringTemplate getMailBody() {
        return new StringTemplate(bodyTemplate);
    }

    String getMailBodyType() {
        return this.bodyType;
    }

    List<Attachment> getAttachments() {
        return this.attachments;
    }

    private void addRecipientList(Element element, Message.RecipientType recipientType) {
        Collection<Element> addressElements = XMLHelper.getChildrenWithTagName(element, "address");
        for (Element addressElement : addressElements) {
            String email = XMLHelper.getContent(addressElement, null, "");
            String name = addressElement.getAttribute("name");
            recipients.add(new Recipient(email, name, recipientType));
        }
    }

    private void addAttachments(Element element) {
        Element attachmentsElement = XMLHelper.getElement(element, "attachments");
        if (attachmentsElement != null) {
            Collection<Element> attachmentElements = XMLHelper.getChildrenWithTagName(attachmentsElement, "attachment");
            for (Element attachmentElement : attachmentElements) {
                attachments.add(new Attachment(attachmentElement.getAttribute("name"), XMLHelper.getContent(attachmentElement, null, "")));
            }
        }
    }

    @Override
    public SMTPCall createInstance(ProcessTaskActor processTaskActor) {
        return new SMTPCall(processTaskActor, this);
    }
}
