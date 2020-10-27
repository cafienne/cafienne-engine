/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.cafienne.akka.actor.command.exception.InvalidCommandException;
import org.cafienne.cmmn.definition.casefile.CaseFileDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.expression.spel.SpelReadable;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CaseFile extends CaseFileItemCollection<CaseFileDefinition> implements SpelReadable {
    public CaseFile(Case caseInstance, CaseFileDefinition definition) {
        super(caseInstance, definition, "CASEFILE " + definition.getName());
    }

    /**
     * Returns the case file items within this case file
     *
     * @return
     */
    public Map<CaseFileItemDefinition, CaseFileItem> getCaseFileItems() {
        return getItems();
    }

    private ValueMap validateValueMap(Value<?> newContent) {
        if (newContent instanceof ValueMap) {
            ValueMap items = (ValueMap) newContent;
            items.fieldNames().forEachRemaining(name -> {
                if (getItem(name) == null) {
                    throw new InvalidCommandException("A case file item with name '" + name + "' is not defined");
                }
            });
            return (ValueMap) newContent;
        }
        throw new InvalidCommandException("Operations on entire case file need a JSON object structure");
    }

    public void createContent(Value<?> newContent) {
        validateValueMap(newContent).getValue().entrySet().forEach(entry -> getItem(entry.getKey()).createContent(entry.getValue()));
    }

    public void deleteContent() {
        getItems().values().forEach(CaseFileItem::deleteContent);
    }

    public void replaceContent(Value<?> newContent) {
        validateValueMap(newContent).getValue().entrySet().forEach(entry -> getItem(entry.getKey()).replaceContent(entry.getValue()));
    }

    public void updateContent(Value<?> newContent) {
        validateValueMap(newContent).getValue().entrySet().forEach(entry -> getItem(entry.getKey()).updateContent(entry.getValue()));
    }

    public void validateTransition(CaseFileItemTransition intendedTransition, Value<?> newContent) {
        if (intendedTransition == CaseFileItemTransition.Delete) {
            // Deleting entire case file is allowed under all circumstances...
            return;
        }
        validateValueMap(newContent).getValue().entrySet().stream().forEach(entry -> {
            String itemName = entry.getKey();
            Value newItemContent = entry.getValue();
            CaseFileItem item = getItem(itemName);
            item.validateTransition(intendedTransition, newItemContent);
//            if (!item.validateTransition(intendedTransition, newItemContent)) {
//                throw new InvalidCommandException(intendedTransition + " CaseFile cannot be done because item " + itemName + " is in state " + item.getState());
//            }
            // Also validate the properties and children of the new value
//            item.getDefinition().validate(newItemContent);
        });
    }

    /**
     * Returns the items within this CaseFile.
     *
     * @return
     */
    public Map<CaseFileItemDefinition, CaseFileItem> getItems() {
        return super.getItems();
    }

    /**
     * Traverses the case file along the path and returns the corresponding case file item.
     * If the case file item does not yet exist, one will be created (without a value).
     *
     * @param path
     * @return
     */
    public CaseFileItem getItem(Path path) {

        // TODO: this code belongs in the super class

        if (path.getName().isEmpty())
            return null; // It is top level.
        return getItem(path, path.getRoot(), getCaseInstance(), this);
    }

    public ValueMap toJson() {
        ValueMap caseFileJson = new ValueMap();
        getItems().values().forEach(item -> caseFileJson.put(item.getName(), item.getValue()));
        return caseFileJson;
    }

    @Override
    public Value<?> read(String propertyName) {
        return getItem(propertyName).getValue();
    }

    @Override
    public boolean canRead(String propertyName) {
        return getChildDefinition(propertyName) != null;
    }

    public void dumpMemoryStateToXML(Element parentElement) {
        Element caseFileXML = parentElement.getOwnerDocument().createElement("CaseFile");
        parentElement.appendChild(caseFileXML);
        Iterator<Entry<CaseFileItemDefinition, CaseFileItem>> c = getCaseFileItems().entrySet().iterator();
        while (c.hasNext()) {
            c.next().getValue().dumpMemoryStateToXML(caseFileXML);
        }
    }

    @Override
    public String toString() {
        Document xmlDocument;
        try {
            xmlDocument = XMLHelper.loadXML("<CaseFile />");
            this.dumpMemoryStateToXML(xmlDocument.getDocumentElement());
        } catch (Exception willNotOccur) {
            throw new RuntimeException("Cannot parse xml???", willNotOccur);
        }
        String caseFileString = XMLHelper.printXMLNode(xmlDocument.getDocumentElement().getFirstChild());
        return caseFileString;
    }
}