package org.cafienne.processtask.implementation.http.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.processtask.definition.Resolver;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.cafienne.processtask.implementation.http.Header;
import org.w3c.dom.Element;

public class HeaderDefinition extends SubProcessInputMappingDefinition {
    private final Resolver nameResolver;
    private final Resolver valueResolver;

    public HeaderDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.nameResolver = parseResolver("name", false, "");
        this.valueResolver = this.getResolver();
    }

    public Header getHeader(APIRootObject<?> context) {
        String value = valueResolver.getValue(context);
        String name = nameResolver.getValue(context, "");
        return new Header(name, value);
    }
}
