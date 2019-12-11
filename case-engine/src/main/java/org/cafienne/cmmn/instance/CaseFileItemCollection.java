/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import java.util.LinkedHashMap;
import java.util.Map;

import org.cafienne.cmmn.definition.Multiplicity;
import org.cafienne.cmmn.definition.casefile.CaseFileItemCollectionDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaseFileItemCollection<T extends CaseFileItemCollectionDefinition> extends CMMNElement<T> {
    private final static Logger logger = LoggerFactory.getLogger(CaseFileItemCollection.class);

    private final Map<CaseFileItemDefinition, CaseFileItem> items = new LinkedHashMap<CaseFileItemDefinition, CaseFileItem>();
    private final String name;
    public final int instanceNumber;
    private static int instanceCounter = 0;

    protected CaseFileItemCollection(Case caseInstance, T definition, String name) {
        super(caseInstance, definition);
        this.name = name;
        this.instanceNumber = instanceCounter++;
    }

    /**
     * Returns the case file item name (taken from it's definition)
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the items within this container.
     * @return
     */
    protected Map<CaseFileItemDefinition, CaseFileItem> getItems() {
        return items;
    }
    
    /**
     * Return the child item with the name and specified index. Only works for case file items that have a multiplicity {@link Multiplicity#ZeroOrMore} or {@link Multiplicity#OneOrMore},
     * will throw an {@link InvalidPathException} for the other types.
     * @param childName
     * @param index
     * @return
     */
    public CaseFileItem getItem(String childName, int index) {
        return getItem(childName).getItem(index); // By default return the item; array will overwrite this method
    }
    
    /**
     * Returns the case file item with the specified index. Default implementation throws an exception, i.e., invoking this method
     * on a plain case file item will result in an {@link InvalidPathException}. It can only be invoked properly on a CaseFileItemArray.
     * @param index
     * @return
     */
    protected CaseFileItem getItem(int index) {
        throw new InvalidPathException("This is not an iterable case file item");
    }

    /**
    * Returns the case file item with the specified name, or null if it does not exist
    * @param childName
    * @return
    */
    public CaseFileItem getItem(String childName) {
        CaseFileItemDefinition childDefinition = getChildDefinition(childName);
        if (childDefinition == null) {
            return null;
        }
        return getItem(childDefinition);
    }

    protected CaseFileItem getItem(CaseFileItemDefinition childDefinition) {
        CaseFileItem item = getItems().get(childDefinition);
        if (item == null)
        {
            // Does not yet exist, so create it. Without setting a value or transitioning it into the Available state!
            item = childDefinition.createInstance(getCaseInstance(), this);
            getItems().put(childDefinition, item);
        }
        return item;
    }

    protected CaseFileItem getItem(Path destinationPath, Path currentPath, Case caseInstance, CaseFileItemCollection<?> parent) {
        CaseFileItem currentItem = getItem(currentPath.getCaseFileItemDefinition(), caseInstance, parent);
        currentItem = currentItem.resolve(currentPath); // Hmmm, should be done more elegantly, i guess

        // Now, check to see if we have reached our destination, and if so, return our item
        if (currentPath.getChild() == null) {
            return currentItem;
        } else { // go get the item in our children
            return getItem(destinationPath, currentPath.getChild(), caseInstance, currentItem);
        }
    }

    private CaseFileItem getItem(CaseFileItemDefinition caseFileItemDefinition, Case caseInstance, CaseFileItemCollection<?> parent) {
        Map<CaseFileItemDefinition, CaseFileItem> itemCollection = parent.getItems();

        for (CaseFileItem caseFileItem : itemCollection.values()) {
            if (caseFileItem.getDefinition().equals(caseFileItemDefinition)) {
                return caseFileItem;
            }
        }
        // Does not yet exist, so create it. Without setting a value!
        CaseFileItem item = caseFileItemDefinition.createInstance(caseInstance, parent);
        itemCollection.put(caseFileItemDefinition, item);
        return item;
    }

    /**
     * Returns a child definition that has the specified name or identifier if it exists for this case file item.
     * @param childName
     * @return
     */
    public CaseFileItemDefinition getChildDefinition(String childName) {
        CaseFileItemDefinition childDefinition = getDefinition().getItems().stream().filter(d -> {
            return d.getName().equals(childName);
        }).findFirst().orElse(null);
        return childDefinition;
    }

}
