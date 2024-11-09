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

    protected CMMNElementDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement, boolean... identifierRequired) {
        super(element, modelDefinition, parentElement, identifierRequired);
        this.documentation = parseDocumentation();
        if (modelDefinition != null) {
            modelDefinition.addCMMNElement(this);
        }
    }

    /**
     * If documentation is not present, we'll create dummy place holder to avoid nullpointerexceptions when reading the documentation.
     * Note that the dummy placeholder also converts a potential CMMN1.0 "description" attribute if that is still present
     *
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
     * Returns the documentation object of the element
     *
     * @return
     */
    public CMMNDocumentationDefinition getDocumentation() {
        return this.documentation;
    }

    public String toString() {
        if (getName().isEmpty()) {
            return getClass().getSimpleName();
        } else {
            return getName();
        }
    }

    protected StageDefinition getSurroundingStage() {
        CMMNElementDefinition ancestor = this.getParentElement();
        while (ancestor != null && !(ancestor instanceof StageDefinition)) {
            ancestor = ancestor.getParentElement();
        }
        return (StageDefinition) ancestor;
    }

    public static <T extends CMMNElementDefinition> T fromJSON(String sourceClassName, ValueMap json, Class<T> tClass) {
        String guid = json.readString(Fields.elementId);
        String source = json.readString(Fields.source);
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
