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

import org.cafienne.cmmn.actorapi.event.file.*;
import org.cafienne.cmmn.actorapi.event.migration.CaseFileItemDropped;
import org.cafienne.cmmn.actorapi.event.migration.CaseFileItemMigrated;
import org.cafienne.cmmn.definition.CMMNElementDefinition;
import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.Path;
import org.cafienne.cmmn.instance.State;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.cafienne.cmmn.instance.sentry.TransitionGenerator;
import org.cafienne.json.Value;
import org.cafienne.json.ValueMap;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CaseFileItem extends CaseFileItemCollection<CaseFileItemDefinition> implements TransitionGenerator<CaseFileItemTransitioned> {
    /**
     * History of events on this item
     */
    protected CaseFileTransitionPublisher transitionPublisher;
    private State state = State.Null; // Current state of the item
    private CaseFileItemTransition lastTransition; // Last transition

    private Value<?> value = Value.NULL;
    private final Map<String, BusinessIdentifier> businessIdentifiers = new HashMap<>();
    /**
     * The parent case file item that we are contained in, or null if we are contained in the top level case file.
     */
    private final CaseFileItem parent;
    /**
     * The container holding this item - either the item itself, or the array in which it exists
     */
    private final CaseFileItem container;
    /**
     * Our location in a {@link CaseFileItemArray}, if we belong to one. Else -1.
     */
    private int indexInArray;
    /**
     * Shortcut to know our type of class
     */
    private final boolean isArray;

    private CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent, CaseFileItemArray array, int indexInArray, boolean isArray) {
        super(caseInstance, definition, parent);
        this.parent = parent instanceof CaseFileItem ? (CaseFileItem) parent : null;
        this.container = array == null ? this : array;
        this.indexInArray = indexInArray;
        this.transitionPublisher = createTransitionPublisher();
        this.isArray = isArray;
        for (PropertyDefinition property : definition.getBusinessIdentifiers()) {
            businessIdentifiers.put(property.getName(), new BusinessIdentifier(this, property));
        }
        getCaseInstance().getSentryNetwork().connect(this);
    }

    /**
     * Constructor for CaseFileItems that belong to an array
     *
     * @param array
     * @param indexInArray
     */
    protected CaseFileItem(CaseFileItemArray array, int indexInArray) {
        this(array.getCaseInstance(), array.getDefinition(), array.getParent(), array, indexInArray, false);
    }

    /**
     * Constructor for {@link CaseFileItemArray}.
     *
     * @param caseInstance
     * @param definition
     * @param parent
     */
    protected CaseFileItem(CaseFileItemDefinition definition, Case caseInstance, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1, true);
    }

    /**
     * Constructor for plain CaseFileItems (i.e., not belonging to an array)
     *
     * @param caseInstance
     * @param definition
     * @param parent
     */
    public CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1, false);
    }

    @Override
    public void releaseBootstrapEvents() {
        // From now onwards we can handle case file events the regular way
        // Creating the normal publisher will release events from bootstrap publisher
        transitionPublisher = new CaseFileTransitionPublisher(getPublisher());
        // Now release child item bootstrap events
        super.releaseBootstrapEvents();
    }

    private CaseFileTransitionPublisher createTransitionPublisher() {
        // If the case plan is not yet available, we need to preserve the case file events and
        //  only trigger entry/exit criteria after case plan has become available.
        //  This happens in the release method.
        if (getCaseInstance().getCasePlan() == null) {
            // NOTE: weirdly enough the case file item events before bootstrap need to be done on the array instead of the item. Unclear why ...
            //  We use this.container => this is either the CaseFileItemArray or a non-array-element CaseFileItem
            if (this.container.getPublisher() == null) {
                this.container.transitionPublisher = new BootstrapCaseFileTransitionPublisher(this.container);
            }
            return this.container.getPublisher();
        } else {
            // NOTE: see above note: apparently normal transition publisher does not use the parent array...
            return new CaseFileTransitionPublisher(this);
        }
    }

    /**
     * Returns the parent of this case file item, or null if this is a top level item (i.e., it is a child of the casefile)
     *
     * @return
     */
    public CaseFileItem getParent() {
        return parent;
    }

    public boolean isArray() {
        return isArray;
    }

    public <C extends CaseFileItem> C getContainer() {
        return (C) container;
    }

    /**
     * Links the on part to this case file item. Is used by the case file item to connect the connected criterion whenever a transition happens.
     *
     * @param onPart
     */
    public void connectOnPart(CaseFileItemOnPart onPart) {
        getPublisher().connectOnPart(onPart);
    }

    public void releaseOnPart(CaseFileItemOnPart onPart) {
        getPublisher().releaseOnPart(onPart);
    }

    private void addCaseFileEvent(CaseFileItemTransitioned event) {
        if (event.getValue().isMap()) {
            this.businessIdentifiers.values().forEach(bi -> bi.update(event.getValue().asMap()));
        }
        super.addEvent(event);
    }

    private void addDeletedEvent(CaseFileItemDeleted event) {
        this.businessIdentifiers.values().forEach(BusinessIdentifier::clear);
        super.addEvent(event);
    }

    public void publishTransition(CaseFileItemTransitioned event) {
        addDebugInfo(() -> "CaseFile[" + getName() + "]: updating CaseFileItem state based on CaseFileEvent");
        this.getPublisher().addEvent(event);
    }

    public void updateStandardEvent(CaseFileItemTransitioned event) {
        // Hack to make sure that we use the right case file item in case of BootstrapPublisher,
        // as that is registered on the array instead of the item that received the event.
        CaseFileItem item = this instanceof CaseFileItemArray ? ((CaseFileItemArray) this).get(event.getIndex()) : this;

        item.setState(event.getState());
        item.indexInArray = event.getIndex();
        item.lastTransition = event.getTransition();
        item.setValue(event.getValue());
    }

    public void publishTransition(BusinessIdentifierEvent event) {
        businessIdentifiers.get(event.name).updateState(event);
    }

    public void informConnectedEntryCriteria(CaseFileItemTransitioned event) {
        // Inform the activating sentries
        getPublisher().informEntryCriteria(event);
    }

    public void informConnectedExitCriteria(CaseFileItemTransitioned event) {
        // Finally iterate the terminating sentries and inform them
        getPublisher().informExitCriteria(event);
        addDebugInfo(() -> getDescription() + ": Completed behavior for transition " + event.getTransition());
    }

    @Override
    public void createContent(Value<?> newContent) {
        // EVENT ORDER for Create Content: first create ourselves, then the children
        generateContentWarnings(newContent, "Create");
        addCaseFileEvent(new CaseFileItemCreated(this, newContent));
        if (newContent.isMap()) {
            newContent.asMap().getValue().forEach((name, newChildValue) -> {
                CaseFileItem child = getItem(name);
                if (child != null) {
                    child.createContent(newChildValue.cloneValueNode());
                }
            });
        }
    }

    @Override
    public void replaceContent(Value<?> newContent) {
        // EVENT ORDER for Replace Content: first replace children, then ourselves

        if (newContent.equals(value)) {
            addDebugInfo(() -> "Replace on CaseFileItem[" + getPath() + "] does not contain changes");
            return;
        }
        generateContentWarnings(newContent, "Replace");
        if (newContent == Value.NULL) {
            // Replace all existing, Available children with "null"
            getDefinition().getChildren().forEach(childDefinition -> {
                CaseFileItem item = getItem(childDefinition.getName());
                if (item.getState().isAvailable()) {
                    item.deleteContent();
                }
            });
        } else if (newContent.isMap()) {
            ValueMap map = newContent.asMap();
            // Replace new content found in the map
            getDefinition().getChildren().forEach(childDefinition -> {
                String childName = childDefinition.getName();
                if (map.has(childName)) {
                    Value<?> newChildValue = map.get(childName);
                    CaseFileItem item = getItem(childName);
                    if (item.getState().isAvailable()) {
                        item.replaceContent(newChildValue);
                    } else if (item.getState().isNull() && map.has(childName)) {
                        item.createContent(map.get(childName));
                    }
                }
            });
            // Now remove children not found in the map
            removeReplacedItems(map);
        }
        addCaseFileEvent(new CaseFileItemReplaced(this, newContent));
    }

    private void generateContentWarnings(Value<?> newContent, String op) {
        addDebugInfo(() -> {
            if (newContent.isMap()) {
                // Filter properties that are not defined
                List<String> undefinedProperties = newContent.asMap().getValue().keySet().stream().filter(this::isUndefined).collect(Collectors.toList());
                if (undefinedProperties.size() == 1) {
                    return op + " on CaseFileItem[" + getPath() + "] contains undefined property '" + undefinedProperties.get(0) + "'";
                } else if (undefinedProperties.size() > 1) {
                    return op + " on CaseFileItem[" + getPath() + "] contains undefined properties " + undefinedProperties.stream().map(p -> "'" + p + "'").collect(Collectors.joining(", "));
                }
            } else {
                if (getDefinition().getCaseFileItemDefinition().getProperties().size() > 0) {
                    return op + " on CaseFileItem[" + getPath() + "] is done with a value of type " + newContent.getValue().getClass().getSimpleName() + "; a Map<Name,Value> is expected instead.";
                }
            }
            return "";
        });
    }

    @Override
    public void updateContent(Value<?> newContent) {
        // EVENT ORDER for Update Content: first update children, then ourselves (optionally)

        if (value.isSupersetOf(newContent)) {
            addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] does not contain changes");
            return;
        }

        // First check whether we should be triggering Create or Update.
        //  For Create logic is straightforward, for Update we need to do little more.
        if (this.getState().isNull()) {
            createContent(newContent);
            return;
        }

        // Now check type of value. If not a ValueMap, simply merge our value with new value and return;
        // Note: Value.merge returns the parameter if types are not equal, so if newContent is not a ValueMap and existing is,
        //  then this will replace existing content map with the new value type. Not sure if this is handy behavior,
        //  but at least it is erroneous invocation ;)
        if (!newContent.isMap() || !value.isMap()) {
            addDebugInfo(() -> {
                if (newContent.isMap()) {
                    return "Update on CaseFileItem[" + getPath() + "] overwrites value of type " + value.getClass().getSimpleName() + " with a ValueMap";
                } else { // Current content is (probably?) a value map - which is overwritten by something else
                    if (newContent.isList() && this.container != this) {
                        return "Update on CaseFileItem[" + getPath() + "] is overwritten with a list. This seems to be an error on passing the path, but it is accepted";
                    }
                    if (!getDefinition().getCaseFileItemDefinition().getProperties().isEmpty()) {
                        return "Update on CaseFileItem[" + getPath() + "] overwrites existing properties with a single " + value.getClass().getSimpleName() + ". This seems to be an error, but it is accepted";
                    } else {
                        return "Update on CaseFileItem[" + getPath() + "] overwrites a " + value.getClass().getSimpleName() + " with a " + newContent.getClass().getSimpleName();
                    }
                }
            });
            Value<?> newValue = value.cloneValueNode().merge(newContent);
            addCaseFileEvent(new CaseFileItemUpdated(this, newValue));
            return;
        }

        ValueMap newMap = newContent.asMap();
        ValueMap ourMap = value.asMap();

        // Now iterate our children and check if the newContent has values for them as well, and if
        //  they are changed, then we will first update those children.
        getDefinition().getChildren().stream().map(CMMNElementDefinition::getName).forEach(childName -> {
            if (newMap.has(childName)) {
                Value<?> newChildValue = newMap.get(childName);
                CaseFileItem childItem = getItem(childName);
                Value<?> existingChildValue = childItem.getValue();
                if (!existingChildValue.isSupersetOf(newChildValue)) {
                    childItem.updateContent(newChildValue);
                }
            }
        });

        // Iterate the properties in the new content (skip the children as they are handled above)
        // If there is a difference with our current value, update the property.
        // Accept undefined properties as well, but generate a debug warning for those.
        ValueMap updatedProperties = new ValueMap();
        newMap.getValue().forEach((propertyName, newPropertyValue) -> {
            if (getItem(propertyName) != null) {
                // This is actually a child. Let's skip it, because it is handled above.
                return;
            }
            Value<?> currentPropertyValue = ourMap.get(propertyName);
            if (!currentPropertyValue.isSupersetOf(newPropertyValue)) {
                if (getDefinition().getCaseFileItemDefinition().getProperties().get(propertyName) == null) {
                    addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] contains property '" + propertyName + "'. This property is NOT DEFINED in the CaseDefinition");
                }
                updatedProperties.put(propertyName, newPropertyValue);
            }
        });

        // Only make a transition if there are changed properties.
        if (!updatedProperties.getValue().isEmpty()) {
            addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] contains changes in properties " + updatedProperties.getValue().keySet().stream().map(p -> "'" + p + "'").collect(Collectors.joining(", ")));
            Value<?> newValue = value.cloneValueNode().merge(updatedProperties);
            addCaseFileEvent(new CaseFileItemUpdated(this, newValue));
        } else {
            addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] has no property changes");
        }
    }

    @Override
    public void deleteContent() {
        // EVENT ORDER for Delete Content: first delete children, then ourselves (optionally)

        // First recursively delete all of our 'Available' children...
        getItems().stream().filter(item -> item.getState().isAvailable()).forEach(CaseFileItem::deleteContent);
        // Only generate the event if we're not yet in discarded state.
        if (getState() != State.Discarded) addDeletedEvent(new CaseFileItemDeleted(this));
    }

    /**
     * Sets the value of this CaseFileItem. Internal framework method not to be used from applications.
     *
     * @param newValue
     */
    protected void setValue(Value<?> newValue) {
        addDebugInfo(() -> "Setting case file item [" + getPath() + "] value to: ", newValue);
        // Remove ownership from former value, set our value, and also tell the value that we now own it.
        this.value.clearOwner();
        this.value = newValue;
        this.value.setOwner(this);

        // Now update our parent chain (including the array if we belong to one),
        // to make sure the parent's value map is up-to-date with our value
        //  Note: if we are in an array, then we need to update the array and
        //  propagate the whole array value instead of just our own value
        this.container.itemChanged(this);
        propagateValueChangeToParent(getName(), this.container.getValue());
    }

    /**
     * Hook to inform array (if we belong to one) about our change.
     *
     * @param item
     */
    protected void itemChanged(CaseFileItem item) {
    }

    /**
     * Hook to inform array (if we belong to one) about removal of a child.
     *
     * @param index
     */
    protected void itemRemoved(int index) {
    }

    /**
     * This updates the json structure of the parent CaseFileItem, without triggering CaseFileItemTransitions
     *
     * @param childName
     * @param childValue
     */
    private void propagateValueChangeToParent(String childName, Value<?> childValue) {
        if (parent != null) {
            if (parent.value == null || parent.value == Value.NULL) {
                addDebugInfo(() -> "Creating a location in parent " + parent.getPath() + " to store the newly changed child " + getName());
                // Setting parent value will propagate the changes further up if needed.
                parent.setValue(new ValueMap(childName, childValue));
            } else if (parent.value.isMap()) {
                // Check whether we need to change our value in the parent. E.g. for arrays, if a new item is added
                //  to the array, the value in the parent will not change (it is same ValueList - with new item)
                ValueMap parentMap = parent.value.asMap();
                if (parentMap.get(childName) != childValue) {
                    // Ah. We have a real new value. Let's update it in our parent
                    parentMap.put(childName, childValue);
                }
            } else {
                addDebugInfo(() -> "Cannot propagate change in " + getPath() + " into parent, because it's value is not a ValueMap but a " + parent.value.getClass().getName());
            }
        }
    }

    /**
     * Returns the content of the CaseFileItem
     *
     * @return
     */
    public Value<?> getValue() {
        return value;
    }

    /**
     * Returns the last known transition on this case file item
     *
     * @return
     */
    public CaseFileItemTransition getLastTransition() {
        return lastTransition;
    }

    /**
     * Returns a path to this case file item.
     *
     * @return
     */
    public Path getPath() {
        return new Path(this);
    }

    /**
     * Returns the current state of the case file item.
     *
     * @return
     */
    public State getState() {
        return state;
    }

    /**
     * Change the state of this item.
     * Note: Arrays have special way of handling state.
     * Items in it have their own state, complying with the standard.
     * Array also has a state helping to understand how to deal with operations that slightly deviates
     * (e.g., Create can be done on an array in state "Available" and results in adding an element)
     *
     * @param newState
     */
    protected void setState(State newState) {
        this.state = newState;
    }

    @Override
    public CaseFileTransitionPublisher getPublisher() {
        return transitionPublisher;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Dump the CaseFile item as XML.
     *
     * @param parentElement
     */
    public void dumpMemoryStateToXML(Element parentElement) {
        Element caseFileItemXML = parentElement.getOwnerDocument().createElement("CaseFileItem");
        parentElement.appendChild(caseFileItemXML);
        caseFileItemXML.setAttribute("name", getDefinition().getName());
        caseFileItemXML.setAttribute("transition", "" + lastTransition);
        if (indexInArray >= 0) {
            caseFileItemXML.setAttribute("index", "" + indexInArray);
        }

        // First print our contents (includes probably also content of our children...)
        Element contentElement = parentElement.getOwnerDocument().createElement("Content");
        caseFileItemXML.appendChild(contentElement);
        value.dumpMemoryStateToXML(contentElement);

        // Finally, print our children.
        getItems().forEach(item -> item.dumpMemoryStateToXML(caseFileItemXML));
    }

    /**
     * Reference to most recently updated case file item. Returns <code>this</code> by default. CaseFileItemArray overwrites it
     * and returns the most recently changed / updated case file item in it's array
     *
     * @return
     */
    public CaseFileItem getCurrent() {
        return this;
    }

    /**
     * Returns the location of this case file item within the array it belongs to. If the case file item is not "iterable", then -1 is returned.
     *
     * @return
     */
    public int getIndex() {
        return indexInArray;
    }

    @Override
    public void validateTransition(CaseFileItemTransition intendedTransition, Value<?> content) {
        if (parent != null) {
            if (parent.getState().isDiscarded() || parent.getState().isNull()) {
                throw new CaseFileError(intendedTransition + "CaseFileItem[" + getPath() + "] cannot be done because the parent is in state " + parent.getState());
            }
        }
        // Validate current state against transition
        if (!allowTransition(intendedTransition)) {
            throw new CaseFileError(intendedTransition + "CaseFileItem[" + getPath() + "] cannot be done because item is in state " + getState());
        }

        // Validate type of new content
        getDefinition().validatePropertyTypes(content);
    }

    protected boolean allowTransition(CaseFileItemTransition intendedTransition) {
        switch (getState()) {
            case Null:
                // Also support upserting a case file item; note: we explicitly do NOT check whether the parent CFI is in state Available. Not sure if that is required...
                return intendedTransition == CaseFileItemTransition.Create || intendedTransition == CaseFileItemTransition.Update || intendedTransition == CaseFileItemTransition.Replace;
            case Available:
                return intendedTransition == CaseFileItemTransition.Update || intendedTransition == CaseFileItemTransition.Replace || intendedTransition == CaseFileItemTransition.Delete;
            default: {
                addDebugInfo(() -> "CFI[" + getPath() + "] is in state " + getState() + " and then we cannot do transition " + intendedTransition);
                return false;
            }
        }
    }

    @Override
    public void migrateDefinition(CaseFileItemDefinition newDefinition, boolean skipLogic) {
        addDebugInfo(() -> "=== Migrating CaseFileItem[" + getPath() + "] to a new definition");
        super.migrateDefinition(newDefinition, skipLogic);
        migrateIdentifiers(newDefinition, skipLogic);
        addDebugInfo(() -> "=== Completed migration of CaseFileItem[" + getPath() + "]\n");
    }

    private void migrateIdentifiers(CaseFileItemDefinition newDefinition, boolean skipLogic) {
        // All identifiers will be removed, unless ...
        Map<String, BusinessIdentifier> identifiersToRemove = new HashMap<>(businessIdentifiers);
        newDefinition.getBusinessIdentifiers().forEach(newPropertyDefinition -> {
            String name = newPropertyDefinition.getName();
            BusinessIdentifier identifier = businessIdentifiers.get(name);
            if (identifier == null) {
                if (skipLogic) return;

                // Let's add a new one
                BusinessIdentifier newIdentifier = new BusinessIdentifier(this, newPropertyDefinition);
                businessIdentifiers.put(name, newIdentifier);
                if (this.getValue().isMap()) {
                    newIdentifier.update(this.getValue().asMap());
                }
            } else {
                // Migrate the identifier to the new definition
                identifier.migrateDefinition(newPropertyDefinition, skipLogic);
                // Make sure this one will not be deleted.
                identifiersToRemove.remove(name);
            }
        });

        if (skipLogic) return;

        // Tell these identifiers to get lost.
        identifiersToRemove.values().forEach(BusinessIdentifier::lostDefinition);
    }

    protected void lostDefinition() {
        // Also tell children to get dropped, and then only tell ourselves.
        getItems().forEach(CaseFileItem::lostDefinition);
        addEvent(new CaseFileItemDropped(this));
    }

    @Override
    protected void childDropped(CaseFileItem child) {
        // Tell collection to remove child from the items list.
        super.childDropped(child);
        // Also remove the child from our value (if it is a map, otherwise the child will not exist in it)
        if (value.isMap()) {
            value.asMap().getValue().remove(child.getName());
        }
    }

    public void updateState(CaseFileItemDropped event) {
        host.childDropped(this); // Inform our host (whether CaseFile or CaseFileItem) about us leaving the grid.
        businessIdentifiers.values().forEach(BusinessIdentifier::lostDefinition);
        getCaseInstance().getSentryNetwork().disconnect(this);
    }

    public void updateState(CaseFileItemMigrated event) {
        if (parent != null) {
            parent.renameChildItem(event.formerPath.getName(), event.path.getName());
        }
    }

    public void migrateName(CaseFileItemDefinition newChildDefinition) {
        addEvent(new CaseFileItemMigrated(this));
    }

    protected void renameChildItem(String formerName, String newName) {
        if (getValue().isMap()) {
            ValueMap map = getValue().asMap();
            Value<?> childValue = map.get(formerName);
            map.put(newName, childValue);
            map.getValue().remove(formerName);
        }
    }
}
