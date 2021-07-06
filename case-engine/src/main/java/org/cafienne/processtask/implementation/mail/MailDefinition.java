/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.mail;

import org.cafienne.infrastructure.Cafienne;
import org.cafienne.json.StringValue;
import org.cafienne.json.ValueList;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.cafienne.util.StringTemplate;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 */
public class MailDefinition extends SubProcessDefinition {
    private final StringTemplate subject;
    private final BodyTemplate body;
    private final List<AddressTemplate> toList = new ArrayList<>();
    private final List<AddressTemplate> ccList = new ArrayList<>();
    private final List<AddressTemplate> bccList = new ArrayList<>();
    private final AddressTemplate from;
    private final AddressTemplate replyTo;
    private final List<AttachmentTemplate> attachmentList = new ArrayList<>();

    public MailDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.subject = parseTemplate("subject", false);
        this.body = parse("mail-body", BodyTemplate.class, false);
        parseGrandChildren("to", "address", AddressTemplate.class, toList);
        parseGrandChildren("cc", "address", AddressTemplate.class, ccList);
        parseGrandChildren("bcc", "address", AddressTemplate.class, bccList);
        this.from = parse("from", AddressTemplate.class, false);
        this.replyTo = parse("reply-to", AddressTemplate.class, false);
        parseGrandChildren("attachments", "attachment", AttachmentTemplate.class, attachmentList);
    }

    @Override
    public Set<String> getRawOutputParameterNames() {
        // Only exception parameters...
        return super.getExceptionParameterNames();
    }

    public Properties getMailProperties() {
        Properties properties = Cafienne.config().engine().mailService().asProperties();
        return properties;
    }

    @Override
    public Mail createInstance(ProcessTaskActor processTaskActor) {
        return new Mail(processTaskActor, this);
    }

    public ValueMap convert(ValueMap input) {
        // Template Parameters
        ValueMap mailParameters = input.cloneValueNode();

        convertList(toList, input, mailParameters, "to");
        convertList(ccList, input, mailParameters, "cc");
        convertList(bccList, input, mailParameters, "bcc");
        convertTemplate(from, input, mailParameters, "from");
        convertTemplate(replyTo, input, mailParameters, "replyTo");
        convertString(subject, input, mailParameters, "subject");
        if (body != null) {
            mailParameters.put("body", body.resolve(input));
        }
        if (! attachmentList.isEmpty()) {
            attachmentList.forEach(attachmentTemplate -> mailParameters.withArray("attachments").add(attachmentTemplate.resolve(input)));
        }

        // raw parameters required = {
        // x  to:
        // x  cc:
        // x  bcc:
        // x  from:
        // x  replyTo:
        // x  subject:
        // x  body:
        // x  attachments:
        //   invite:
        //  }

        return mailParameters;
    }

    private void convertString(StringTemplate template, ValueMap input, ValueMap target, String type) {
        if (template == null) {
            // Nothing to convert!
            return;
        }
        target.put(type, new StringValue(template.resolveParameters(input).toString()));
    }

    private void convertList(List<AddressTemplate> templates, ValueMap input, ValueMap target, String type) {
        if (templates.isEmpty()) {
            // Nothing to convert!
            return;
        }
        ValueList list = target.withArray(type);
        toList.forEach(address -> list.add(resolveAddress(address, input)));
    }

    private void convertTemplate(AddressTemplate template, ValueMap input, ValueMap target, String type) {
        if (template == null) {
            // Nothing to convert!
            return;
        }
        target.put(type, resolveAddress(template, input));
    }

    private ValueMap resolveAddress(AddressTemplate template, ValueMap input) {
        return new ValueMap("name", template.getName(input), "email", template.getEmail(input));
    }
}
