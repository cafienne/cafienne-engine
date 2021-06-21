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
}
