/*
 * Copyright (C) 2022 Batav B.V. <https://www.batav.com/cafienne-enterprise>
 */

package org.cafienne.processtask.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.cafienne.cmmn.expression.spel.api.process.InputMappingRoot;
import org.cafienne.processtask.instance.ProcessTaskActor;
import org.w3c.dom.Element;

/**
 * Creates a Resolver on the content of the XML element.
 */
public class SubProcessInputMappingDefinition extends CMMNElementDefinition {
    private final Resolver resolver;

    protected SubProcessInputMappingDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        this(element, modelDefinition, parentElement, false);
    }

    protected SubProcessInputMappingDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement, boolean identifierRequired) {
        super(element, modelDefinition, parentElement, identifierRequired);
        String source = parseString(null, false, "");
        this.resolver = createResolver(source);
    }

    protected Resolver parseResolver(String attributeName, boolean presenceRequired, String... defaultValue) {
        String attribute = parseAttribute(attributeName, presenceRequired, defaultValue);
        return createResolver(attribute);
    }

    protected Resolver createResolver(String source) {
//        System.out.println("Reading source expresssion in a " + this.getClass().getSimpleName() + " gives " + source);
        return new Resolver(this, source);
    }

    public String getSource() {
        return resolver.getSource();
    }

    public Resolver getResolver() {
        return resolver;
    }

    @Override
    public String getContextDescription() {
        String className = this.getClass().getSimpleName();
        if (className.endsWith("Definition")) {
            return className.substring(0, className.length() - "Definition".length());
        } else {
            return className;
        }
    }

    /**
     * Shortcut metchanism on this.getResolver
     */
    @SafeVarargs
    public final <T> T resolve(APIRootObject<?> root, T... defaultValue) {
        return resolver.getValue(root, defaultValue);
    }

    @SafeVarargs
    public final <T> T resolve(ProcessTaskActor task, T... defaultValue) {
        return resolve(new InputMappingRoot(task), defaultValue);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameMappingDefinition);
    }

    public boolean sameMappingDefinition(SubProcessInputMappingDefinition other) {
        return same(resolver, other.resolver);
    }
}
