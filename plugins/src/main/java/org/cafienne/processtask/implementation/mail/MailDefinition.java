package org.cafienne.processtask.implementation.mail;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class MailDefinition extends com.casefabric.processtask.implementation.mail.MailDefinition {
    public MailDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }
}
