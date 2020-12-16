/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
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
public class ModelDefinition extends CMMNElementDefinition {
    private final String defaultExpressionLanguage;
    /**
     * Collection with all elements belonging to the case definition.
     */
    private final Collection<CMMNElementDefinition> elements = new ArrayList();
    /**
     * Document to which this definition belongs.
     */
    private final DefinitionsDocument document;

    private final Map<String, InputParameterDefinition> inputParameters = new LinkedHashMap();
    private final Map<String, OutputParameterDefinition> outputParameters = new LinkedHashMap();

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
        return findElement(element -> element.getName().equals(identifier) || element.getId().equals(identifier));
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
}
