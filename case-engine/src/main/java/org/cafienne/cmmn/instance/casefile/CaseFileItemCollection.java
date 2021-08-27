/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import org.cafienne.cmmn.actorapi.event.file.CaseFileItemChildRemoved;
import org.cafienne.cmmn.definition.casefile.CaseFileItemCollectionDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.State;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class CaseFileItemCollection<T extends CaseFileItemCollectionDefinition> extends CMMNElement<T> {
    /**
     * Child items of this collection.
     */
    private final List<CaseFileItem> items = new ArrayList<>();

    protected CaseFileItemCollection(Case caseInstance, T definition) {
        super(caseInstance, definition);
    }

    private CaseFileItem constructItem(CaseFileItemDefinition childDefinition) {
        CaseFileItem item = childDefinition.createInstance(getCaseInstance(), this);
        items.add(item);
        return item;
    }

    private void removeItem(CaseFileItem item) {
        items.remove(item);
    }

    /**
     * Returns a copy of the current items within this container.
     *
     * @return
     */
    protected List<CaseFileItem> getItems() {
        return new ArrayList<>(items);
    }

    /**
     * Returns the case file item name (taken from it's definition)
     *
     * @return
     */
    public String getName() {
        return getDefinition().getName();
    }

    /**
     * Case input parameters are bound to the CaseFile. This is done before CasePlan creation,
     * hence entry/exit criteria in the CasePlan do not yet listen to the CaseFile.
     * To overcome this, these CaseFileEvents are not published to the sentry network
     * unless and until this call is done.
     */
    public void releaseBootstrapEvents() {
        getItems().forEach(CaseFileItem::releaseBootstrapEvents);
    }

    /**
     * Returns the case file item with the specified index. Default implementation throws an exception, i.e., invoking this method
     * on a plain case file item will result in an {@link InvalidPathException}. It can only be invoked properly on a CaseFileItemArray.
     *
     * @param index
     * @return
     */
    public CaseFileItem getArrayElement(int index) {
        throw new InvalidPathException("This is not an iterable case file item");
    }

    /**
     * Returns the case file item with the specified name, or null if it does not exist
     *
     * @param childName
     * @return
     */
    public CaseFileItem getItem(String childName) {
        CaseFileItemDefinition childDefinition = getDefinition().getChild(childName);
        if (childDefinition == null) {
            return null;
        }
        for (CaseFileItem item : getItems()) {
            if (item.getDefinition().equals(childDefinition)) return item;
        }
        // If we reach this point, the item does not yet exist, so create it.
        // Note: without setting a value or transitioning it into the Available state!
        return constructItem(childDefinition);
    }

    /**
     * Returns true if the an item (or property) is undefined.
     *
     * @param identifier
     * @return
     */
    protected boolean isUndefined(String identifier) {
        return getDefinition().isUndefined(identifier);
    }

    public abstract void createContent(Value<?> newContent);

    public abstract void deleteContent();

    public abstract void replaceContent(Value<?> newContent);

    /**
     * When replacing existing content with the map, it should generate removeChild events for existing children not in map.
     *
     * @param map
     */
    protected void removeReplacedItems(ValueMap map) {
        Set<String> newKeys = map.getValue().keySet();
        List<CaseFileItem> unfoundItems = getItems().stream().filter(item -> !newKeys.contains(item.getName())).collect(Collectors.toList());
        unfoundItems.forEach(this::removeChildItem);
    }

    protected void removeChildItem(CaseFileItem child) {
        addEvent(new CaseFileItemChildRemoved(this, child.getPath()));
    }

    public void updateState(CaseFileItemChildRemoved event) {
        Path childPath = event.getChildPath();
        CaseFileItem child = getItem(childPath.getName());
        if (childPath.isArrayElement()) {
            child.getContainer().itemRemoved(childPath.index);
        } else {
            removeItem(child);
        }
    }

    public abstract void updateContent(Value<?> newContent);

    public abstract void validateTransition(CaseFileItemTransition intendedTransition, Value<?> newContent);

    public State getState() {
        return State.Available;
    }

    public int getIndex() {
        return -1;
    }

    public Path getPath() {
        return new Path("");
    }

    @Override
    public void migrateDefinition(T newDefinition) {
        super.migrateDefinition(newDefinition);
        MigDevConsole("CFI[" + getPath() + "] gets a new definition");
        Map<String, CaseFileItemDefinition> newItemsByName = newDefinition.getChildren().stream().collect(Collectors.toMap(CaseFileItemDefinition::getName, item -> item));
        Map<String, CaseFileItemDefinition> newItemsById = newDefinition.getChildren().stream().collect(Collectors.toMap(CaseFileItemDefinition::getId, item -> item));
        getItems().forEach(child -> {
            CaseFileItemDefinition childDefinition = child.getDefinition();
            // First check if we can find the child in the new definition by it's existing name....
            CaseFileItemDefinition newChildDefinition = newItemsByName.get(childDefinition.getName());
            if (newChildDefinition != null) {
                // Found the new definition for the child by it's existing name. Simply invoke "migrate" on it.
                child.migrateDefinition(newChildDefinition);
            } else {
                // Since we cannot find the child by name, let's try to find it by id.
                newChildDefinition = newItemsById.get(childDefinition.getId());
                if (newChildDefinition != null) {
                    // We found the child, migrate it. Rename logic to be added
                    // TODO: Also replace name inside the value map and generate an event for it.
                    MigDevConsole("Migrating child definition and name: CFI[" + childDefinition.getName()+"] --> CFI["+newChildDefinition.getName()+"]");
                    child.migrateDefinition(newChildDefinition);
                    child.migrateName(newChildDefinition);
                } else {
                    // We can also not find the child by id. That means we can simply drop it.
                    //  Alternatively we might check whether there's a child in the new definition that has the exact same properties and children
                    //  but perhaps that's more for a migration dsl...
                    child.lostDefinition();
                }
            }
        });
    }

    protected void lostDefinition() {
        // Now what?
        //  - remove value?
        //  - remove business identifiers for the child and it's children ...
        getItems().forEach(CaseFileItem::lostDefinition);
    }

    protected void renameChildItem(String formerName, String newName) {
        // Base implementation is empty, since CaseFile needs not change the name, as it does not keep track of the name (other than through definition)
    }
}
