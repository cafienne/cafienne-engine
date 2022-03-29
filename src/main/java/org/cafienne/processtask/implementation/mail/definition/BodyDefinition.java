package org.cafienne.processtask.implementation.mail.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.processtask.definition.Resolver;
import org.cafienne.processtask.definition.SubProcessInputMappingDefinition;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

public class BodyDefinition extends SubProcessInputMappingDefinition {
    private final Resolver bodyResolver;
    private final Resolver bodyTypeResolver;

    public BodyDefinition(Element element, ModelDefinition definition, CMMNElementDefinition parentElement) {
        super(element, definition, parentElement);
        this.bodyResolver = this.getResolver();
        this.bodyTypeResolver = parseResolver("type", false, "text/html");
    }

    public String getBody(ProcessTaskActor task) {
        return this.bodyResolver.getValue(task, "");
    }

    public String getBodyType(ProcessTaskActor task) {
        return bodyTypeResolver.getValue(task, "text/html");
    }
}
