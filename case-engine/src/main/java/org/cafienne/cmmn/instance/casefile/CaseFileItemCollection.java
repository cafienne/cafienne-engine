/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.event.file.CaseFileItemChildRemoved;
import org.cafienne.cmmn.definition.Multiplicity;
import org.cafienne.cmmn.definition.casefile.CaseFileItemCollectionDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.State;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class CaseFileItemCollection<T extends CaseFileItemCollectionDefinition> extends CMMNElement<T> {
    private final Map<CaseFileItemDefinition, CaseFileItem> items = new LinkedHashMap();
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
     * Case input parameters are bound to the CaseFile. This is done before CasePlan creation,
     * hence entry/exit criteria in the CasePlan do not yet listen to the CaseFile.
     * To overcome this, these CaseFileEvents are not published to the sentry network
     * unless and until this call is done.
     */
    public void releaseBootstrapEvents() {
        new LinkedHashMap<>(getItems()).values().forEach(item -> item.releaseBootstrapEvents());
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

    /**
     * Returns true if the an item (or property) is undefined.
     * @param identifier
     * @return
     */
    protected boolean isUndefined(String identifier) {
        return getDefinition().isUndefined(identifier);
    }

    protected CaseFileItem getItem(CaseFileItemDefinition childDefinition) {
        CaseFileItem item = getItems().get(childDefinition);
        if (item == null) {
            // Does not yet exist, so create it. Without setting a value or transitioning it into the Available state!
            item = childDefinition.createInstance(getCaseInstance(), this);
            getItems().put(childDefinition, item);
        }
        return item;
    }

    /**
     * Returns a child definition that has the specified name or identifier if it exists for this case file item.
     * @param childName
     * @return
     */
    public CaseFileItemDefinition getChildDefinition(String childName) {
        return getDefinition().getChildren().stream().filter(d -> d.getName().equals(childName)).findFirst().orElse(null);
    }

    public abstract void createContent(Value<?> newContent);
    public abstract void deleteContent();
    public abstract void replaceContent(Value<?> newContent);

    /**
     * When replacing existing content with the map, it should generate removeChild events for existing children not in map.
     * @param map
     */
    protected void removeReplacedItems(ValueMap map) {
        Set<String> newKeys = map.getValue().keySet();
        Set<CaseFileItem> unfoundItems = getItems().values().stream().filter(item -> !newKeys.contains(item.getName())).collect(Collectors.toSet());
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
            getItems().remove(child.getDefinition());
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
