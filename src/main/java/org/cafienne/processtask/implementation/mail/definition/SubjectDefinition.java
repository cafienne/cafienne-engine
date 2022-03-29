package org.cafienne.processtask.implementation.mail.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.w3c.dom.Element;

public class SubjectDefinition extends SubProcessInputMappingDefinition {
    public SubjectDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }

}
