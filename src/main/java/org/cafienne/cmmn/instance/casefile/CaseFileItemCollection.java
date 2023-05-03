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

import org.cafienne.cmmn.actorapi.event.file.CaseFileItemChildRemoved;
import org.cafienne.cmmn.definition.casefile.CaseFileItemCollectionDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Path;
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
    protected final CaseFileItemCollection<?> host; // Either the case file or the parent item of this collection

    protected CaseFileItemCollection(Case caseInstance, T definition, CaseFileItemCollection<?> host) {
        super(caseInstance, definition);
        this.host = host;
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
        // Let's first iterate our existing items for a child with this name.
        //  Reason: when recovering the event on a dropped case file item
        //  the new definition no longer has this child, leading to recovery errors.
        //  The item still exists, but not it's definition. Therefore we first check the existing items.
        for (CaseFileItem item : getItems()) {
            if (item.getName().equals(childName)) return item;
        }
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
    public void migrateDefinition(T newDefinition, boolean skipLogic) {
        super.migrateDefinition(newDefinition, skipLogic);
        Map<String, CaseFileItemDefinition> newItemsByName = newDefinition.getChildren().stream().collect(Collectors.toMap(CaseFileItemDefinition::getName, item -> item));
        Map<String, CaseFileItemDefinition> newItemsById = newDefinition.getChildren().stream().collect(Collectors.toMap(CaseFileItemDefinition::getId, item -> item));
        getItems().forEach(child -> {
            CaseFileItemDefinition childDefinition = child.getDefinition();
            // First check if we can find the child in the new definition by it's existing name....
            CaseFileItemDefinition newChildDefinition = newItemsByName.get(childDefinition.getName());
            if (newChildDefinition != null) {
                // Found the new definition for the child by it's existing name. Simply invoke "migrate" on it.
                child.migrateDefinition(newChildDefinition, skipLogic);
            } else {
                if (skipLogic) return;

                // Since we cannot find the child by name, let's try to find it by id.
                newChildDefinition = newItemsById.get(childDefinition.getId());
                if (newChildDefinition != null) {
                    // We found the child, migrate name first, and then the item itself and it's children.
                    String newName = newChildDefinition.getName();
                    addDebugInfo(() -> "Migrating child name '" + childDefinition.getName() + "' to '" + newName + "'");
                    child.migrateDefinition(newChildDefinition, skipLogic);
                    child.migrateName(newChildDefinition);
                } else {
                    // We can also not find the child by id. That means we can simply drop it.
                    //  Alternatively we might check whether there's a child in the new definition that has the exact same properties and children
                    //  but perhaps that's more for a migration dsl...
                    addDebugInfo(() -> "Dropping child CaseFileItem[" + child.getPath() + "]");
                    child.lostDefinition();
                }
            }
        });
    }

    protected void childDropped(CaseFileItem child) {
        items.remove(child);
    }

    protected void renameChildItem(String formerName, String newName) {
        // Base implementation is empty, since CaseFile needs not change the name, as it does not keep track of the name (other than through definition)
    }
}
