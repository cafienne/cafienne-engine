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

package org.cafienne.processtask.definition;

import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.ModelDefinition;
import org.cafienne.cmmn.expression.spel.Resolver;
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
        super(element, modelDefinition, parentElement);
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
        return getType();
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
