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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinitionDefinition;
import org.cafienne.cmmn.definition.parameter.InputParameterDefinition;
import org.cafienne.cmmn.definition.parameter.OutputParameterDefinition;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base class for top-level elements inside the <code>&lt;definitions&gt;</code> tag, such as
 * {@link CaseDefinition}, {@link ProcessDefinition}, {@link CaseFileItemDefinitionDefinition}, etc.
 */
public abstract class ModelDefinition extends CMMNElementDefinition {
    private final String defaultExpressionLanguage;
    /**
     * Collection with all elements belonging to the case definition.
     */
    private final Collection<CMMNElementDefinition> elements = new ArrayList<>();
    /**
     * Document to which this definition belongs.
     */
    private final DefinitionsDocument document;

    private final Map<String, InputParameterDefinition> inputParameters = new LinkedHashMap<>();
    private final Map<String, OutputParameterDefinition> outputParameters = new LinkedHashMap<>();

    protected ModelDefinition(Element element, DefinitionsDocument document) {
        super(element, null, null, true); // All definitions must have an identifier
        this.document = document;
        // Yes, we know, it is not supposed to be defined at the individual case definition level, but that is way more handy when e.g. the processes use a different language...
        this.defaultExpressionLanguage = parseAttribute("expressionLanguage", false, document.getDefaultExpressionLanguage());

        parse("input", InputParameterDefinition.class, inputParameters);
        parse("output", OutputParameterDefinition.class, outputParameters);
        document.addElement(this);
    }

    /**
     * Returns the default expression language used in this model.
     * If that is not defined, it will take if from the DefinitionsDocument;
     *
     * @return
     */
    public String getDefaultExpressionLanguage() {
        return defaultExpressionLanguage;
    }

    public DefinitionsDocument getDefinitionsDocument() {
        return document;
    }

    /**
     * Technical method for definition parser to keep track of elements within this model for easy lookup.
     *
     * @param cmmnElementDefinition
     */
    void addCMMNElement(CMMNElementDefinition cmmnElementDefinition) {
        elements.add(cmmnElementDefinition);
        document.addElement(cmmnElementDefinition);
    }

    @FunctionalInterface
    public interface ElementMatcher {
        boolean matches(CMMNElementDefinition element);
    }

    /**
     * Returns the element with the given identifier if it exists; does a cast for you.
     *
     * @param matcher a functional interface {@link ElementMatcher} providing a boolean filter function on CMMNElementDefinition
     * @return
     */
    public <T extends CMMNElementDefinition> T findElement(ElementMatcher matcher) {
        for (CMMNElementDefinition element : elements) {
            if (matcher.matches(element)) return (T) element;
        }
        return null;
    }

    /**
     * Returns the element with the given identifier if it exists; does a cast for you.
     *
     * @param identifier
     * @return
     */
    public <T extends CMMNElementDefinition> T getElement(String identifier) {
        return findElement(element -> element.hasIdentifier(identifier));
    }

    @Override
    public ModelDefinition getModelDefinition() {
        // Note: this override is required, since CMMNElementDefinition needs the ModelDefinition in it's constructor, and we cannot pass 'this' when
        // invoking the super constructor
        return this;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();

        // Now iterate all elements and tell them to resolve their references
        for (CMMNElementDefinition element : elements) {
            element.resolveReferences();
        }
    }

    @Override
    protected void validateElement() {
        super.validateElement();

        // Now iterate all elements and tell them to validate
        for (CMMNElementDefinition element : elements) {
            element.validateElement();
        }
    }

    /**
     * During parsing of the definition, errors may be encountered. These can be registered on the case through this method without needing to immediately terminate the parsing process (e.g. by throwing
     * a java exception)
     *
     * @param msg
     */
    public void addDefinitionError(String msg) {
        document.addDefinitionError(this, msg);
    }

    public void addReferenceError(String msg) {
        document.addDefinitionError(this, msg);
    }

    public void fatalError(String msg, Throwable t) {
        document.addFatalError(this, msg, t);
    }

    /**
     * Returns the map with the input parameters of this case
     *
     * @return
     */
    public Map<String, InputParameterDefinition> getInputParameters() {
        return inputParameters;
    }

    /**
     * Returns the map with the output parameters of this case
     *
     * @return
     */
    public Map<String, OutputParameterDefinition> getOutputParameters() {
        return outputParameters;
    }

    public boolean sameModelDefinition(ModelDefinition other) {
        return sameIdentifiers(other)
                && same(defaultExpressionLanguage, other.defaultExpressionLanguage)
                && same(inputParameters.values(), other.inputParameters.values())
                && same(outputParameters.values(), other.outputParameters.values());
    }
}
