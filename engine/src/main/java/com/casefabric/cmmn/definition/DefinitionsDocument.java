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

package com.casefabric.cmmn.definition;

import com.casefabric.cmmn.definition.casefile.CaseFileItemDefinitionDefinition;
import com.casefabric.cmmn.definition.casefile.ImportDefinition;
import com.casefabric.cmmn.repository.MissingDefinitionException;
import com.casefabric.processtask.definition.ProcessDefinition;
import com.casefabric.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class DefinitionsDocument implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(DefinitionsDocument.class);

    private final String sourceDocument;
    private String defaultExpressionLanguage;

    /**
     * XML Parsed version of the source document string.
     */
    private transient Document document;
    /**
     * All definition objects, e.g. Case, Process, CaseFileItemDefinition, etc.
     */
    private transient Collection<ModelDefinition> definitions = new ArrayList<>();
    /**
     * All elements is a collection of all the elements across all Definition documents
     */
    private transient Collection<CMMNElementDefinition> allElements;

    /**
     * During parsing of the case, multiple errors may be encountered. We try to parse the case as long as possible, and give as much feedback to the case developer as we can, so that he can see all
     * errors at once, instead of needing to fix error for error and in between having to parse each time.
     */
    private transient Collection<String> definitionErrors = new ArrayList<>();
    private transient Collection<InvalidDefinitionException> fatals = new ArrayList<>();

    /**
     * Creates a new DefinitionsDocument based on the given XML Document.
     *
     * @param document XML document to be parsed into a DefinitionsDocument
     * @throws InvalidDefinitionException If the XML content of the file is not conforming to the CMMN specification.
     */
    public DefinitionsDocument(Document document) throws InvalidDefinitionException {
        this.document = document;

        // Take a copy of the definition, in order to be able to store it in a database.
        // This is to be able to continue running with the exact same definition
        // after being re-loaded into memory after server crash or so.
        this.sourceDocument = XMLHelper.printXMLNode(document);

        // Now parse the various definitions
        // Parsing of the definition happens in 2 rounds. First, the definition is parsed straight away, and it may also
        // parse it's children. In the second phase of parsing, for all the elements inside the definition, the resolveReferences method is
        // invoked.
        // Both phases can result in errors, and we try to collect as many errors as we can.
        // However, if after the first phase already errors have been found, the second phase is not initiated, as the second phase depends too
        // much on a proper outcome of the first phase, and errors in the first phase are usually very technical.

        init();
    }

    private DefinitionsDocument(String source) throws InvalidDefinitionException {
        this.document = null;
        this.sourceDocument = source;
        init();
    }

    public static DefinitionsDocument fromSource(String source) {

        try {
            return new DefinitionsDocument(source);
        } catch (InvalidDefinitionException e) {
            throw new RuntimeException("Cannot deserialize DefinitionsDocument from the source", e);
        }
    }

    private void init() throws InvalidDefinitionException {
        if (document == null) { // document might be null after deserialization
            try {
                document = XMLHelper.loadXML(sourceDocument);
            } catch (IOException | ParserConfigurationException | SAXException e) {
                logger.error("Could not parse a definitions document that has been parsed before?!", e);
                throw new RuntimeException("Could not parse the XML that was parsable before!?", e);
            }
        }

        // Parse the default expression language. If the attribute is not defined, then it will set an empty string
        this.defaultExpressionLanguage = document.getDocumentElement().getAttribute("expressionLanguage");

        allElements = new ArrayList<>();

        parseImports();
        parseCaseFileItemDefinitions();
        parseProcessDefinitions();
        parseCaseDefinitions();

        checkForErrors();

        for (ModelDefinition definition : getDefinitions()) {
            definition.resolveReferences();
        }

        checkForErrors();

        for (ModelDefinition definition : getDefinitions()) {
            definition.validateElement();
        }

        checkForErrors();
    }

    /**
     * Returns the default expression language used in this set of models.
     * If not defined, it returns an empty string.
     *
     * @return
     */
    public String getDefaultExpressionLanguage() {
        return defaultExpressionLanguage;
    }

    private void initAfterDeserialization() {
        try {
            init();
        } catch (InvalidDefinitionException e) {
            logger.error("Could not parse a definitions document that has been parsed before?!", e);
            throw new RuntimeException("Could not parse the XML that was parsable before!?", e);
        }
    }

    /**
     * Returns the first case in the definitions file. Typically the main case when deployed using CaseFabric IDE
     *
     * @return The first case in the definitions file, or throws a {@link MissingDefinitionException} if none is there.
     * @throws MissingDefinitionException if there is no case definition inside this definitions document.
     */
    public CaseDefinition getFirstCase() throws MissingDefinitionException {
        for (ModelDefinition definition : getDefinitions()) {
            if (definition instanceof CaseDefinition) {
                return (CaseDefinition) definition;
            }
        }
        throw new MissingDefinitionException("The definitions document does not contain a case definition");
    }

    private void parseImports() {
        Collection<Element> typeElements = XMLHelper.getChildrenWithTagName(getDocument().getDocumentElement(), "import");
        for (Element element : typeElements) {
            ModelDefinition definition = new ImportDefinition(element, this);
            getDefinitions().add(definition);
        }
    }

    private void parseCaseFileItemDefinitions() {
        Collection<Element> typeElements = XMLHelper.getChildrenWithTagName(getDocument().getDocumentElement(), "caseFileItemDefinition");
        for (Element element : typeElements) {
            ModelDefinition definition = new CaseFileItemDefinitionDefinition(element, this);
            getDefinitions().add(definition);
        }
    }

    private void parseProcessDefinitions() {
        Collection<Element> typeElements = XMLHelper.getChildrenWithTagName(getDocument().getDocumentElement(), "process");
        for (Element element : typeElements) {
            ModelDefinition definition = new ProcessDefinition(element, this);
            getDefinitions().add(definition);
        }
    }

    private void parseCaseDefinitions() {
        Collection<Element> typeElements = XMLHelper.getChildrenWithTagName(getDocument().getDocumentElement(), "case");
        for (Element element : typeElements) {
            ModelDefinition definition = new CaseDefinition(element, this);
            getDefinitions().add(definition);
        }
    }

    private void checkForErrors() throws InvalidDefinitionException {
        if (getDefinitions().isEmpty()) {
            getDefinitionErrors().add("The definitions document does not contain any definitions");
        }
        if (!getDefinitionErrors().isEmpty()) {
            throw new InvalidDefinitionException(definitionErrors);
        } else if (!getFatals().isEmpty()) {
            throw fatals.iterator().next(); // Hmmm, should try to throw 'm all. But even the first is already disastrous
        }
    }

    /**
     * Returns a serialized version of the XML document through which this definition was created
     *
     * @return
     */
    public String getSource() {
        return sourceDocument;
    }

    /**
     * Returns the XML document representing the unparsed DefinitionsDocument.
     *
     * @return
     */
    public Document getDocument() {
        if (document == null) { // document might be null after deserialization
            initAfterDeserialization();
        }

        return document;
    }

    private Collection<ModelDefinition> getDefinitions() {
        if (null == definitions) {
            definitions = new ArrayList<>();
            initAfterDeserialization();
        }

        return definitions;
    }

    void addElement(CMMNElementDefinition element) {
        allElements.add(element);
    }

    /**
     * Returns the element with the given identifier if it exists; does a cast for you.
     *
     * @param matcher a functional interface {@link ModelDefinition.ElementMatcher} providing a boolean filter function on CMMNElementDefinition
     * @return
     */
    public <T extends CMMNElementDefinition> T findElement(ModelDefinition.ElementMatcher matcher) {
        for (CMMNElementDefinition element : allElements) {
            if (matcher.matches(element)) return (T) element;
        }
        return null;
    }

    /**
     * Returns the element with the given identifier if it exists; does a cast for you.
     *
     * @param guid   The id that the element
     * @param tClass The class that the element must match with
     * @return
     */
    public <T extends CMMNElementDefinition> T getElement(String guid, Class<T> tClass) {
        return findElement(e -> e.hasIdentifier(guid) && tClass.isAssignableFrom(e.getClass()));
    }

    private Collection<String> getDefinitionErrors() {
        if (null == definitionErrors) {
            definitionErrors = new ArrayList<>();
            initAfterDeserialization();
        }
        return definitionErrors;
    }

    private Collection<InvalidDefinitionException> getFatals() {
        if (null == fatals) {
            fatals = new ArrayList<>();
            initAfterDeserialization();
        }
        return fatals;
    }

    private <T extends ModelDefinition> T getDefinition(Class<T> typeClass, String identifier) {
        for (ModelDefinition definition : getDefinitions()) {
            if (definition.hasIdentifier(identifier)) {
                if (typeClass.isAssignableFrom(definition.getClass())) {
                    @SuppressWarnings("unchecked") // We just did the checking, right?!
                    T typedDefinition = (T) definition;
                    return typedDefinition;
                }
            }
        }
        return null;
    }

    /**
     * Return the case definition with the specified identifier.
     *
     * @param identifier The name or id of the case to be parsed.
     * @return The definition, or null if it cannot be found
     */
    public CaseDefinition getCaseDefinition(String identifier) {
        return getDefinition(CaseDefinition.class, identifier);
    }

    /**
     * Return the process definition with the specified identifier.
     *
     * @param identifier The name or id of the process to be parsed
     * @return The definition, or null if it cannot be found
     */
    public ProcessDefinition getProcessDefinition(String identifier) {
        return getDefinition(ProcessDefinition.class, identifier);
    }

    /**
     * Return the case file item definition with the specified identifier.
     *
     * @param identifier The name or id of the case file item to be parsed
     * @return The definition, or null if it cannot be found
     */
    public CaseFileItemDefinitionDefinition getCaseFileItemDefinition(String identifier) {
        return getDefinition(CaseFileItemDefinitionDefinition.class, identifier);
    }

    /**
     * Return the import definition with the specified identifier.
     *
     * @param identifier The name or id of the import to be parsed
     * @return The definition, or null if it cannot be found
     */
    public ImportDefinition getImportDefinition(String identifier) {
        return getDefinition(ImportDefinition.class, identifier);
    }

    void addDefinitionError(ModelDefinition source, String msg) {
        getDefinitionErrors().add(source.getId() + ": " + msg);
    }

    void addFatalError(ModelDefinition source, String msg, Throwable t) {
        getFatals().add(new InvalidDefinitionException(source.getId() + ": " + msg, t));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefinitionsDocument that = (DefinitionsDocument) o;
        return document == that.document || Objects.equals(sourceDocument, that.sourceDocument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceDocument);
    }
}
