package org.cafienne.processtask.implementation.mail;

import org.cafienne.json.StringValue;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.XMLElementDefinition;
import org.cafienne.util.StringTemplate;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

public class BodyTemplate extends XMLElementDefinition {
    private final String bodyTemplate;
    private final String bodyType;

    public BodyTemplate(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.bodyTemplate = XMLHelper.getContent(element, null, "");
        this.bodyType = parseAttribute("type", false, "");
    }

    Value resolve(ValueMap input) {
        return new StringValue(new StringTemplate(bodyTemplate).resolveParameters(input).getResult());
    }

}
