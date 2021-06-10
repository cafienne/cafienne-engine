package org.cafienne.processtask.implementation.mail;

import org.cafienne.actormodel.serialization.json.ValueMap;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.XMLElementDefinition;
import org.cafienne.util.StringTemplate;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Element;

public class AddressTemplate extends XMLElementDefinition {
    private final StringTemplate email;
    private final StringTemplate name;

    public AddressTemplate(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.email = new StringTemplate(XMLHelper.getContent(element, null, ""));
        this.name = new StringTemplate(parseAttribute("name", false, ""));
    }


    public String getEmail(ValueMap processInputParameters) {
        email.resolveParameters(processInputParameters);
        return email.toString();
    }

    public String getName(ValueMap processInputParameters) {
        name.resolveParameters(processInputParameters);
        return name.toString();
    }
}
