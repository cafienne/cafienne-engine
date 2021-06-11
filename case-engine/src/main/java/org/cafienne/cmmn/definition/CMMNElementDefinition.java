/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.definition;

import org.cafienne.infrastructure.serialization.CafienneSerializer;
import org.cafienne.infrastructure.serialization.DeserializationError;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;
import org.cafienne.processtask.definition.ProcessDefinition;
import org.cafienne.util.XMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * Base class for parsing XML elements defined in the CMMN specification.
 */
public abstract class CMMNElementDefinition extends XMLElementDefinition {
    private final static Logger logger = LoggerFactory.getLogger(CafienneSerializer.class);
    public final CMMNDocumentationDefinition documentation;
    private final String id;
    private String name;

    protected CMMNElementDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement, boolean... identifierRequired) {
        super(element, modelDefinition, parentElement);

        this.name = parseAttribute("name", false);
        this.id = parseAttribute("id", false);
        this.documentation = parseDocumentation();
        if (identifierRequired.length > 0 && identifierRequired[0] == true) {
            if (this.name.isEmpty() && this.id.isEmpty()) {
                getModelDefinition().addDefinitionError("An element of type '" + printElement() + "' does not have an identifier " + XMLHelper.printXMLNode(element));
            }
        }
        if (modelDefinition != null) {
            modelDefinition.addCMMNElement(this);
        }
    }

    /**
     * If documentation is not present, we'll create dummy place holder to avoid nullpointerexceptions when reading the documentation.
     * Note that the dummy placeholder also converts a potential CMMN1.0 "description" attribute if that is still present
     * @return
     */
    private CMMNDocumentationDefinition parseDocumentation() {
        CMMNDocumentationDefinition documentation = parse("documentation", CMMNDocumentationDefinition.class, false);
        if (documentation == null) {
            documentation = new CMMNDocumentationDefinition(this.getModelDefinition(), this);
        }

        return documentation;
    }

    /**
     * Subclasses are allowed to give a different name than what is specified in the element itself.
     *
     * @param name
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the element. Can be used in combination with the id of the element to resolve an XSD IDREF to this element.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the identifier of the element. Can be used in combination with the name of the element to resolve an XSD IDREF to this element.
     *
     * @return
     */
    public String getId() {
        if (this.id == null || this.id.isEmpty()) {
            return this.name;
        } else {
            return this.id;
        }
    }

    /**
     * Returns the documentation object of the element
     *
     * @return
     */
    public CMMNDocumentationDefinition getDocumentation() {
        return this.documentation;
    }

    /**
     * Returns a description of the context this element provides to it's children. Can be used e.g. in expressions or on parts
     * to get the description of the parent element when encountering validation errors.
     *
     * @return
     */
    public String getContextDescription() {
        return "";
    }

    public String toString() {
        if (getName().isEmpty()) {
            return getClass().getSimpleName();
        } else {
            return getName();
        }
    }

    public CaseDefinition getCaseDefinition() {
        return (CaseDefinition) getModelDefinition();
    }

    public ProcessDefinition getProcessDefinition() {
        return (ProcessDefinition) getModelDefinition();
    }

    protected StageDefinition getSurroundingStage() {
        CMMNElementDefinition ancestor = this.getParentElement();
        while (ancestor != null && !(ancestor instanceof StageDefinition)) {
            ancestor = ancestor.getParentElement();
        }
        return (StageDefinition) ancestor;
    }

    public String getType() {
        String simpleName = getClass().getSimpleName();
        if (simpleName.endsWith("Definition")) {
            simpleName = simpleName.substring(0, simpleName.length() - "Definition".length());
        }
        return simpleName;
    }

    public static <T extends CMMNElementDefinition> T fromJSON(String sourceClassName, ValueMap json, Class<T> tClass) {
        String guid = json.raw(Fields.elementId);
        String source = json.raw(Fields.source);
        try {
            DefinitionsDocument def = new DefinitionsDocument(XMLHelper.loadXML(source));
            T element = def.getElement(guid, tClass);
            return element;
        } catch (InvalidDefinitionException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Encountered invalid definition during deserialization; probably content from a newer or older version", e);
            } else {
                logger.warn("Encountered invalid definition during deserialization; probably content from a newer or older version.\nEnable debug logging for full stacktrace. Error messages: " + e.getErrors());
            }
            throw new DeserializationError("Invalid Definition Failure while deserializing an instance of " + sourceClassName, e);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            // TTD we need to come up with a more suitable exception, since this logic is typically also
            //  invoked when recovering from events.
            throw new DeserializationError("Parsing Failure while deserializing an instance of " + sourceClassName, e);
        }
    }

    public ValueMap toJSON() {
        String identifier = this.getId();
        if (identifier == null || identifier.isEmpty()) {
            identifier = this.getName();
        }
        String source = getModelDefinition().getDefinitionsDocument().getSource();
        ValueMap json = new ValueMap(Fields.elementId, identifier, Fields.source, source);
        return json;
    }
}
