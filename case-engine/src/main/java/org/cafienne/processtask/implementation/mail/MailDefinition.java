/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.processtask.implementation.mail;

import org.cafienne.akka.actor.CaseSystem;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.SubProcessDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

import java.util.Properties;
import java.util.Set;

/**
 */
public class MailDefinition extends SubProcessDefinition {
    public MailDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }

    @Override
    public Set<String> getRawOutputParameterNames() {
        // Only exception parameters...
        return super.getExceptionParameterNames();
    }

    Properties getMailProperties() {
        Properties properties = CaseSystem.config().engine().mailService().asProperties();
        return properties;
    }

    @Override
    public Mail createInstance(ProcessTaskActor processTaskActor) {
        return new Mail(processTaskActor, this);
    }
}
