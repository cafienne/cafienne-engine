/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.mail;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.infrastructure.Cafienne;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.implementation.mail.definition.AddressDefinition;
import org.cafienne.processtask.implementation.mail.definition.AttachmentDefinition;
import org.cafienne.processtask.implementation.mail.definition.BodyDefinition;
import org.cafienne.processtask.implementation.mail.definition.SubjectDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 *
 */
public class MailDefinition extends SubProcessDefinition {
    private final SubjectDefinition subject;
    private final BodyDefinition body;
    private final AddressDefinition from;
    private final AddressDefinition replyTo;
    private final List<AddressDefinition> toList = new ArrayList<>();
    private final List<AddressDefinition> ccList = new ArrayList<>();
    private final List<AddressDefinition> bccList = new ArrayList<>();
    private final List<AttachmentDefinition> attachmentList = new ArrayList<>();

    public MailDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.subject = parse("subject", SubjectDefinition.class, false);
        this.body = parse("mail-body", BodyDefinition.class, false);
        this.from = parse("from", AddressDefinition.class, false);
        this.replyTo = parse("reply-to", AddressDefinition.class, false);
        parseGrandChildren("to", "address", AddressDefinition.class, toList);
        parseGrandChildren("cc", "address", AddressDefinition.class, ccList);
        parseGrandChildren("bcc", "address", AddressDefinition.class, bccList);
        parseGrandChildren("attachments", "attachment", AttachmentDefinition.class, attachmentList);
    }

    public SubjectDefinition getSubject() {
        return subject;
    }

    public AddressDefinition getFrom() {
        return from;
    }

    public AddressDefinition getReplyTo() {
        return replyTo;
    }

    public List<AddressDefinition> getToList() {
        return toList;
    }

    public List<AddressDefinition> getCcList() {
        return ccList;
    }

    public List<AddressDefinition> getBccList() {
        return bccList;
    }

    public BodyDefinition getBody() {
        return body;
    }

    public List<AttachmentDefinition> getAttachmentList() {
        return attachmentList;
    }

    @Override
    public Set<String> getRawOutputParameterNames() {
        // Only exception parameters...
        return super.getExceptionParameterNames();
    }

    public Properties getMailProperties() {
        return Cafienne.config().engine().mailService().asProperties();
    }

    @Override
    public Mail createInstance(ProcessTaskActor processTaskActor) {
        return new Mail(processTaskActor, this);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return notYetImplemented();
    }
}
