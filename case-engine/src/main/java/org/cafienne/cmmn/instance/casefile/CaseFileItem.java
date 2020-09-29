/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.casefile;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.akka.event.file.BusinessIdentifierCleared;
import org.cafienne.cmmn.akka.event.file.BusinessIdentifierSet;
import org.cafienne.cmmn.akka.event.file.CaseFileEvent;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.definition.casefile.PropertyDefinition;
import org.cafienne.cmmn.instance.*;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class CaseFileItem extends CaseFileItemCollection<CaseFileItemDefinition> {

    private final List<CaseFileItemOnPart> connectedEntryCriteria = new ArrayList();
    private final List<CaseFileItemOnPart> connectedExitCriteria = new ArrayList();

    /**
     * History of events on this item
     */
    protected TransitionPublisher transitionPublisher;
    private State state = State.Null; // Current state of the item
    private CaseFileItemTransition lastTransition; // Last transition

    private Value<?> value = Value.NULL;
    private Value<?> removedValue = Value.NULL;
    /**
     * The parent case file item that we are contained in, or null if we are contained in the top level case file.
     */
    private final CaseFileItem parent;
    /**
     * The array that this CaseFileItem belongs to (if we have multiplicity "...OrMore"). Otherwise <code>null</code>
     */
    private final CaseFileItemArray array;
    /**
     * Our location in the {@link CaseFileItem#array}, if we belong to one. Else -1.
     */
    private int indexInArray;

    protected CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent, CaseFileItemArray array, int indexInArray) {
        super(caseInstance, definition, definition.getName());
        this.parent = parent instanceof CaseFileItem ? (CaseFileItem) parent : null;
        this.array = array;
        this.indexInArray = indexInArray;
        this.transitionPublisher = createTransitionPublisher();
        getCaseInstance().getSentryNetwork().connect(this);
    }

    @Override
    public void releaseBootstrapEvents() {
        TransitionPublisher bootstrapPublisher = transitionPublisher;
        bootstrapPublisher.releaseBootstrapEvents();
        super.releaseBootstrapEvents();
        // From now onwards we can handle case file events the regular way
        transitionPublisher = new TransitionPublisher(bootstrapPublisher);
    }

    private TransitionPublisher createTransitionPublisher() {
        // If the case plan is not yet available, we need to preserve the case file events and
        //  only trigger entry/exit criteria after case plan has become available.
        //  This happens in the release method.
        if (getCaseInstance().getCasePlan() == null) {
            if (this.array != null) {
                // NOTE: weirdly enough the case file item events before bootstrap need to be done on the array instead of the item. Unclear why ...
                return this.array.transitionPublisher;
            } else {
                return new BootstrapCaseFileTransitionPublisher(this);
            }
        } else {
            // NOTE: see above note: apparently normal transition publisher does not use the parent array...
            return new TransitionPublisher(this);
        }
    }

    public CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1);
    }

    /**
     * Constructor for {@link CaseFileItemArray}.
     *
     * @param caseInstance
     * @param definition
     * @param parent
     */
    protected CaseFileItem(CaseFileItemDefinition definition, Case caseInstance, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1);
    }

    /**
     * Returns the parent of this case file item, or null if this is a top level item (i.e., it is a child of the casefile)
     *
     * @return
     */
    public CaseFileItem getParent() {
        return parent;
    }

    /**
     * Returns the child items of this case file item
     *
     * @return
     */
    public Map<CaseFileItemDefinition, CaseFileItem> getChildren() {
        return getItems();
    }

    /**
     * Links the on part to this case file item. Is used by the case file item to connect the connected criterion whenever a transition happens.
     *
     * @param onPart
     */
    public void connectOnPart(CaseFileItemOnPart onPart) {
        transitionPublisher.connectOnPart(onPart);
    }

    public void releaseOnPart(CaseFileItemOnPart onPart) {
        transitionPublisher.releaseOnPart(onPart);
    }

    /**
     * Framework method that allows parameters to be bound back to the case file
     *
     * @param p
     * @param parameterValue
     */
    public void bindParameter(Parameter<?> p, Value<?> parameterValue) {
        getDefinition().validatePropertyTypes(parameterValue);
        // Spec says (table 5.3.4, page 36): just trigger the proper transition, as that will be obvious. But is it?
        switch (getState()) {
            case Available:
                replaceContent(parameterValue);
                break;
            case Discarded:
                throw new TransitionDeniedException("Cannot bind parameter value, because case file item has been deleted");
            case Null:
                createContent(parameterValue);
                break;
            default:
                break;
        }
    }

    private void removeBusinessIdentifiers(Value value) {
        getDefinition().getBusinessIdentifiers().forEach(property -> {
            addEvent(new BusinessIdentifierCleared(this, property));
        });
    }

    private void updateBusinessIdentifiers(Value newValue) {
        getDefinition().getBusinessIdentifiers().forEach(property -> {
            addEvent(new BusinessIdentifierSet(this, property, getBusinessIdentifierValue(newValue, property)));
        });
    }

    private Value getBusinessIdentifierValue(Value value, PropertyDefinition identifier) {
        if (value.isMap()) {
            return ((ValueMap) value).get(identifier.getName());
        }
        return null;
    }

    private void makeTransition(CaseFileItemTransition transition, State newState, Value newValue) {
        if (transition == CaseFileItemTransition.Delete) {
            removeBusinessIdentifiers(newValue);
        } else {
            updateBusinessIdentifiers(newValue);
        }
        // Now inform the sentry network of our change
        addEvent(new CaseFileEvent(this.getCaseInstance(), this.getName(), newState, transition, newValue, getPath(), indexInArray));
    }

    public void updateState(CaseFileEvent event) {
        addDebugInfo(() -> "CaseFile[" + getName() + "]: updating CaseFileItem state based on CaseFileEvent");
        this.transitionPublisher.addEvent(event);
        this.setState(event.getState());
        this.indexInArray = event.getIndex();
        this.lastTransition = event.getTransition();
        this.setValue(event.getValue());
    }

    public void informConnectedEntryCriteria(CaseFileEvent event) {
        // Finally propagate the changes to children.
        propagateValueChangeToChildren(event);

        // Then inform the activating sentries
        transitionPublisher.informEntryCriteria(event);
    }

    public void informConnectedExitCriteria(CaseFileEvent event) {
        // Finally iterate the terminating sentries and inform them
        transitionPublisher.informExitCriteria(event);
        addDebugInfo(() -> "CaseFile[" + getName() + "]: Completed behavior for transition " + event.getTransition());
    }

    protected void adoptContentFromParent(Value<?> newContentFromParent, CaseFileEvent event) {
        if (getState().equals(State.Null)) {
            createContent(newContentFromParent);
        } else {
            switch (event.getTransition()) {
                case Delete: throw new RuntimeException("Not expecting to reach this code");// This actually should not be happening
                case Replace: {
                    replaceContent(newContentFromParent);
                    break;
                }
                case Update: {
                    updateContent(newContentFromParent);
                    break;
                }
                case Create: {
                    createContent(newContentFromParent);
                    break;
                }
            }
        }
    }

    protected void propagateValueChangeToChildren(CaseFileEvent event) {
        if (event.getTransition() == CaseFileItemTransition.Update) {
            // Update propagation is handled in update method itself.
            return;
        }
        // TODO: For replace we should check whether or not to remove the existing children


        Value newValue = event.getValue();
        if (! newValue.isMap()) {
            return;
        }
        ValueMap v = (ValueMap) newValue;
        v.getValue().forEach((name, newChildValue) -> {
            CaseFileItem child = getItem(name);
            if (child != null) {
                child.adoptContentFromParent(newChildValue.cloneValueNode(), event);
            }
        });
    }

    // TODO: The following 4 methods should generate specific events instead of generic CaseFileEvent event
    public void createContent(Value<?> newContent) {
        makeTransition(CaseFileItemTransition.Create, State.Available, newContent);
    }

    public void replaceContent(Value<?> newContent) {
        makeTransition(CaseFileItemTransition.Replace, State.Available, newContent);
    }

    public void updateContent(Value<?> newContent) {
        if (value.isSupersetOf(newContent)) {
            addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] does not contain changes");
            return;
        }

        // First check whether we should be triggering Create or Update.
        //  For Create logic is straightforward, for Update we need to do little more.
        if (this.getState() == State.Null) {
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
                    return "Update on CaseFileItem[" + getPath() + "] overwrites value of type " + value.getClass().getSimpleName() +" with a ValueMap";
                } else { // Current content is (probably?) a value map - which is overwritten by something else
                    if (newContent.isList() && this.array != null) {
                        return "Update on CaseFileItem[" + getPath() + "] is overwritten with a list. This seems to be an error on passing the path, but it is accepted";
                    }
                    if (!getDefinition().getCaseFileItemDefinition().getProperties().isEmpty()) {
                        return "Update on CaseFileItem[" + getPath() + "] overwrites existing properties with a single " + value.getClass().getSimpleName() +". This seems to be an error, but it is accepted";
                    } else {
                        return "Update on CaseFileItem[" + getPath() + "] overwrites a " + value.getClass().getSimpleName() +" with a " + newContent.getClass().getSimpleName();
                    }
                }
            });
            makeTransition(CaseFileItemTransition.Update, State.Available, value.merge(newContent));
            return;
        }

        ValueMap newMap = (ValueMap) newContent;
        ValueMap ourMap = (ValueMap) value;

        // Now iterate our children and check if the newContent has values for them as well, and if
        //  they are changed, then we will first update those children.
        getDefinition().getChildren().stream().map(childDefinition -> childDefinition.getName()).forEach(childName -> {
            if (newMap.has(childName)) {
                Value newChildValue = newMap.get(childName);
                CaseFileItem childItem = getItem(childName);
                Value existingChildValue = childItem.getValue();
                if (!existingChildValue.isSupersetOf(newChildValue)) {
                    childItem.updateContent(newChildValue);
                }
            }
        });

        // For our properties, merge and make Update (or Create) transition when
        // there is a difference with our current value;
        ValueMap updatedProperties = new ValueMap();
        getDefinition().getCaseFileItemDefinition().getProperties().forEach((propertyName, propertyDefinition) -> {
            if (newMap.has(propertyName)) {
                Value newPropertyValue = newMap.get(propertyName);
                Value currentPropertyValue = ourMap.get(propertyName);
                if (!currentPropertyValue.isSupersetOf(newPropertyValue)) {
                    updatedProperties.put(propertyName, newPropertyValue);
                }
            }
        });

        if (! updatedProperties.getValue().isEmpty()) {
            addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] contains changes in properties " + updatedProperties.getValue().keySet().stream().map(p -> "'" + p + "'").collect(Collectors.joining(", ")));
            makeTransition(CaseFileItemTransition.Update, State.Available, value.merge(newContent));
        } else {
            addDebugInfo(() -> "Update on CaseFileItem[" + getPath() + "] has no property changes");
        }
    }

    public void deleteContent() {
        removedValue = value;
        makeTransition(CaseFileItemTransition.Delete, State.Discarded, Value.NULL);
        // Now recursively also delete all of our children... Are you sure? Isn't this overinterpreting the spec?
        getChildren().forEach((definition, childItem) -> childItem.deleteContent());
    }

    /**
     * Sets the value of this CaseFileItem. Internal framework method not to be used from applications.
     *
     * @param newValue
     */
    protected void setValue(Value<?> newValue) {
        addDebugInfo(() -> "Setting case file item [" + getPath() + "] value", newValue);
        // Set our value, and also tell the value that we now own it.
        this.value = newValue;
        newValue.setOwner(this);

        // Now update our parent chain (including the array if we belong to one),
        // to make sure the parent's value map is up-to-date with our value
        //  Note: if we are in an array, then we need to update the array and
        //  propagate the whole array value instead of just our own value
        if (array != null) {
            this.array.itemChanged(this);
            propagateValueChangeToParent(getName(), array.getValue());
        } else {
            propagateValueChangeToParent(getName(), this.value);
        }
    }

    /**
     * This updates the json structure of the parent CaseFileItem, without triggering CaseFileItemTransitions
     *  @param childName
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
                ValueMap parentMap = (ValueMap) parent.value;
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
     * @param newState
     */
    protected void setState(State newState) {
        this.state = newState;
    }

    @Override
    public String toString() {
        return getName() + " : " + value;
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
        if (array != null) {
            caseFileItemXML.setAttribute("index", "" + indexInArray);
        }

        // First print our contents (includes probably also content of our children...)
        Element contentElement = parentElement.getOwnerDocument().createElement("Content");
        caseFileItemXML.appendChild(contentElement);
        value.dumpMemoryStateToXML(contentElement);

        // Next, print our children.
        Iterator<Entry<CaseFileItemDefinition, CaseFileItem>> c = getChildren().entrySet().iterator();
        while (c.hasNext()) {
            c.next().getValue().dumpMemoryStateToXML(caseFileItemXML);
        }
        // XMLHelper.printXMLNode(parentElement)
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
     * Resolve the path on this case file item. Base implementation that is overridden in {@link CaseFileItemArray}, where the index
     * accessor of the path is used to navigate to the correct case file item.
     *
     * @param currentPath
     * @return
     */
    CaseFileItem resolve(Path currentPath) {
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

    public List<CaseFileItemOnPart> getConnectedEntryCriteria() {
        return connectedEntryCriteria;
    }

    public List<CaseFileItemOnPart> getConnectedExitCriteria() {
        return connectedExitCriteria;
    }

    public boolean allows(CaseFileItemTransition intendedTransition) {
        switch (getState()) {
            case Null: return intendedTransition == CaseFileItemTransition.Create;
            case Available: return intendedTransition == CaseFileItemTransition.Update || intendedTransition == CaseFileItemTransition.Replace || intendedTransition == CaseFileItemTransition.Delete;
            default: {
                addDebugInfo(() ->"CFI[" + getPath() +"] is in state " + getState() +" and then we cannot do transition " + intendedTransition);
                return false;
            }
        }
    }
}
