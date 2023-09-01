package org.cafienne.cmmn.definition.expression;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.expression.spel.Resolver;
import org.cafienne.cmmn.expression.spel.api.APIRootObject;
import org.w3c.dom.Element;

import java.util.Map;

/**
 * Generic base class for an XML element from which the textContent
 * can be parsed, and bound to various different input parameters.
 */
public abstract class ResolverDefinition extends CMMNElementDefinition {
    private final Resolver resolver;

    protected ResolverDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        String source = parseString(null, false, "");
        this.resolver = new Resolver(this, source);
    }

    protected Resolver parseAttributeResolver(String attributeName, String... defaultValue) {
        String source = parseAttribute(attributeName, false, defaultValue);
        return new Resolver(this, source);
    }

    public String getSource() {
        return resolver.getSource();
    }

    /**
     * Returns the map with the input parameters of this case
     */
    public abstract Map<String, InputParameterDefinition> getInputParameters();

    public Resolver getResolver() {
        return resolver;
    }

    @Override
    public String getContextDescription() {
        return getType();
    }

    /**
     * Shortcut mechanism on this.getResolver
     */
    @SafeVarargs
    public final <T> T resolve(APIRootObject<?> root, T... defaultValue) {
        return resolver.getValue(root, defaultValue);
    }

    @Override
    protected boolean equalsWith(Object object) {
        return equalsWith(object, this::sameResolverDefinition);
    }

    public boolean sameResolverDefinition(ResolverDefinition other) {
        return same(resolver, other.resolver);
    }
}