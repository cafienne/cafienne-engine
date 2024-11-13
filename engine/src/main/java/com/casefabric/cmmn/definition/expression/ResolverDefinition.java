/*
 * Copyright (C) 2014  Batav B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.casefabric.cmmn.definition.expression;

import com.casefabric.cmmn.definition.CMMNElementDefinition;
import com.casefabric.cmmn.definition.ModelDefinition;
import com.casefabric.cmmn.definition.parameter.InputParameterDefinition;
import com.casefabric.cmmn.expression.spel.Resolver;
import com.casefabric.cmmn.expression.spel.api.APIRootObject;
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
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameResolverDefinition);
    }

    public boolean sameResolverDefinition(ResolverDefinition other) {
        return same(resolver, other.resolver);
    }
}