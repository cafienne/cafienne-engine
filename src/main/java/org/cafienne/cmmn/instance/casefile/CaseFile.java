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

package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.definition.casefile.CaseFileDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.cafienne.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

public class CaseFile extends CaseFileItemCollection<CaseFileDefinition> {
    public CaseFile(Case caseInstance, CaseFileDefinition definition) {
        super(caseInstance, definition, null);
    }

    /**
     * Returns the case file items within this case file
     *
     * @return
     */
    public List<CaseFileItem> getCaseFileItems() {
        return getItems();
    }

    private ValueMap validateValueMap(Value<?> newContent) {
        if (newContent.isMap()) {
            newContent.asMap().fieldNames().forEachRemaining(name -> {
                if (getItem(name) == null) {
                    throw new CaseFileError("A case file item with name '" + name + "' is not defined");
                }
            });
            return newContent.asMap();
        }
        throw new CaseFileError("Operations on entire case file need a JSON object structure");
    }

    @Override
    public void createContent(Value<?> newContent) {
        validateValueMap(newContent).getValue().entrySet().forEach(entry -> getItem(entry.getKey()).createContent(entry.getValue()));
    }

    @Override
    public void deleteContent() {
        getItems().forEach(CaseFileItem::deleteContent);
    }

    @Override
    public void replaceContent(Value<?> newContent) {
        ValueMap newCaseFileContent = validateValueMap(newContent);
        // Replace new content found in the map
        newCaseFileContent.getValue().entrySet().forEach(entry -> getItem(entry.getKey()).replaceContent(entry.getValue()));
        // Now remove children not found in the map
        removeReplacedItems(newCaseFileContent);
    }

    @Override
    public void updateContent(Value<?> newContent) {
        validateValueMap(newContent).getValue().entrySet().forEach(entry -> getItem(entry.getKey()).updateContent(entry.getValue()));
    }

    @Override
    public void validateTransition(CaseFileItemTransition intendedTransition, Value<?> newContent) {
        if (intendedTransition == CaseFileItemTransition.Delete) {
            // Deleting entire case file is allowed under all circumstances...
            return;
        }
        validateValueMap(newContent).getValue().entrySet().stream().forEach(entry -> {
            String itemName = entry.getKey();
            Value<?> newItemContent = entry.getValue();
            CaseFileItem item = getItem(itemName);
            if (item == null) {
                throw new CaseFileError("Item '" + itemName + "' is not found in the Case File definition");
            }
            item.validateTransition(intendedTransition, newItemContent);
        });
    }

    public ValueMap toJson() {
        ValueMap caseFileJson = new ValueMap();
        getItems().forEach(item -> caseFileJson.put(item.getName(), item.getValue()));
        return caseFileJson;
    }

    public void dumpMemoryStateToXML(Element parentElement) {
        Element caseFileXML = parentElement.getOwnerDocument().createElement("CaseFile");
        parentElement.appendChild(caseFileXML);
        getCaseFileItems().forEach(item -> item.dumpMemoryStateToXML(caseFileXML));
    }

    @Override
    public void migrateDefinition(CaseFileDefinition newDefinition, boolean skipLogic) {
        addDebugInfo(() -> "\nMigrating Case File");
        super.migrateDefinition(newDefinition, skipLogic);
        addDebugInfo(() -> "Completed Case File migration\n");
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