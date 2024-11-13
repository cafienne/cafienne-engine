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

package com.casefabric.processtask.implementation.smtp;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.processtask.implementation.mail.MailDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Properties;

/**
 *
 */
public class SMTPCallDefinition extends MailDefinition {
    private final static Logger logger = LoggerFactory.getLogger(SMTPCallDefinition.class);

    private final String smtpServer;
    private final String smtpPort;

    public SMTPCallDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
        this.smtpServer = parseString("smtp-server", false);
        this.smtpPort = parseString("smtp-port", false);
    }

    public Properties getMailProperties() {
        logger.warn("Using deprecated class to send emails. Please use com.casefabric.processtask.implementation.mail.MailDefinition");

        Properties defaultProperties = super.getMailProperties();
        Properties properties = new Properties();
        properties.putAll(defaultProperties);
        if (this.smtpServer != null) {
            properties.put("mail.host", smtpServer);
            properties.put("mail.smtp.host", smtpServer);
        }
        if (this.smtpPort != null) {
            properties.put("mail.smtp.port", smtpPort);
        }
        return properties;
    }
}
