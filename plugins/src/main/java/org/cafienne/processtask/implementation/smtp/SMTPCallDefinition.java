package org.cafienne.processtask.implementation.smtp;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import org.w3c.dom.Element;

public class SMTPCallDefinition extends com.casefabric.processtask.implementation.smtp.SMTPCallDefinition {
    public SMTPCallDefinition(Element element, ModelDefinition processDefinition, CMMNElementDefinition parentElement) {
        super(element, processDefinition, parentElement);
    }
}
