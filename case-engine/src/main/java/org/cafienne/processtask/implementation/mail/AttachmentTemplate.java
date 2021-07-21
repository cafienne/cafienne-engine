package org.cafienne.processtask.implementation.mail;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.XMLElementDefinition;
import org.cafienne.json.StringValue;
import org.cafienne.json.ValueMap;
import org.cafienne.util.StringTemplate;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

public class AttachmentTemplate extends XMLElementDefinition {
    private final String content;
    private final String name;
    private final String type;

    public AttachmentTemplate(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.content = XMLHelper.getContent(element, null, "");
        this.name = parseAttribute("name", false, "");
        this.type = parseAttribute("type", false, "application/octet-stream");
    }

    ValueMap resolve(ValueMap input) {
        ValueMap output = new ValueMap();
        add(output, input, "fileName", name);
        add(output, input, "content", content);
        add(output, input, "mimeType", type);
        return output;
    }

    private void add(ValueMap output, ValueMap input, String parameterName, String parameter) {
        StringValue value = new StringValue(new StringTemplate(parameter).resolveParameters(input).getResult());
        output.put(parameterName, value);
    }
}
