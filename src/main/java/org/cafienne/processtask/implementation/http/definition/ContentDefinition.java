package org.cafienne.processtask.implementation.http.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.w3c.dom.Element;

public class ContentDefinition extends SubProcessInputMappingDefinition {
    public ContentDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
    }
}
