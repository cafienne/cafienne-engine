/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.smtp;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.implementation.mail.MailDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Properties;

/**
 */
public class SMTPCallDefinition extends MailDefinition {
    private final static Logger logger = LoggerFactory.getLogger(SMTPCallDefinition.class);

    private final String smtpServer;
    private final String smtpPort;

    public SMTPCallDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.smtpServer = parseString("smtp-server", true);
        this.smtpPort = parseString("smtp-port", true);
    }

    public Properties getMailProperties() {
        logger.warn("Using deprecated class to send emails. Please use org.cafienne.processtask.implementation.mail.MailDefinition");

        Properties defaultProperties = super.getMailProperties();
        Properties properties = new Properties();
        properties.putAll(defaultProperties);
        properties.put("mail.host", smtpServer);
        properties.put("mail.smtp.host", smtpServer);
        properties.put("mail.smtp.port", smtpPort);
        return properties;
    }
}
