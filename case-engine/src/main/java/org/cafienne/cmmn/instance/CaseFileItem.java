/* 
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.akka.event.CaseFileEvent;
import org.cafienne.cmmn.akka.event.debug.DebugEvent;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.cmmn.instance.casefile.Value;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.sentry.CaseFileItemOnPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CaseFileItem extends CaseFileItemCollection<CaseFileItemDefinition> {

    private final static Logger logger = LoggerFactory.getLogger(CaseFileItem.class);

    private final List<CaseFileItemOnPart> connectedEntryCriteria = new ArrayList<>();
    private final List<CaseFileItemOnPart> connectedExitCriteria = new ArrayList<>();

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
        getCaseInstance().getSentryNetwork().connect(this);
    }

    public CaseFileItem(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1);
    }

    /**
     * Constructor to be used only from CaseFileItemArray. Case file items created through this constructor do NOT connect
     * to the sentry network, see {@link CaseFileItem#iterator()}.
     * @param caseInstance
     * @param definition
     * @param parent
     */
    protected CaseFileItem(CaseFileItemDefinition definition, Case caseInstance, CaseFileItemCollection<?> parent) {
        this(caseInstance, definition, parent, null, -1);
    }

    /**
     * Returns the parent of this case file item, or null if this is a top level item (i.e., it is a child of the casefile)
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
     * Links the on part to this case file item. Is used by the case file item to connect the connected sentry whenever a transition happens.
     * 
     * @param onPart
     */
    public void connectOnPart(CaseFileItemOnPart onPart) {
        if (onPart.getSentry().isEntryCriterion()) {
            insertOnPart(onPart, connectedEntryCriteria);
        } else {
            insertOnPart(onPart, connectedExitCriteria);
        }
        onPart.inform(this);
    }

    /**
     * Inserts the onPart in the right location of the plan item hierarchy
     * 
     * @param onPart
     * @param list
     */
    private void insertOnPart(CaseFileItemOnPart onPart, List<CaseFileItemOnPart> list) {
        if (list.contains(onPart)) {
            return; // do not connect more than once
        }
        PlanItem onPartStage = onPart.getSentry().getStage().getPlanItem();
        int i = 0;
        // Iterate the list until we encounter an onPart that does not contain the new sentry.
        // TODO: examine this logic as it is copied from PlanItemOnPart. Can we do without?
        while (i < list.size() && list.get(i).getSentry().getStage().contains(onPartStage)) {
            i++;
        }
        list.add(i, onPart);
    }

    /**
     * Framework method that allows parameters to be bound back to the case file
     * @param p
     * @param parameterValue
     */
    void bindParameter(Parameter<?> p, Value<?> parameterValue) {
        // Spec says (table 5.3.4, page 36): just trigger the proper transition, as that will be obvious. But is it?
        switch (state) {
        case Available:
            replaceContent(parameterValue);
            break;
        case Discarded:
            throw new TransitionDeniedException("Cannot bind parameter value, because case file item has been deleted");
        case Null:
            createContent(parameterValue);
        default:
            break;
        }
    }

    private boolean changeState(CaseFileItemTransition transition) {
        if (transition == null)
            return true;

        if (state == State.Null) {
            if (transition == CaseFileItemTransition.Create) {
                state = State.Available;
                return true;
            } else {
                return false;
            }
        } else if (state == State.Available) {
            if (transition == CaseFileItemTransition.Delete) {
                state = State.Discarded;
                return true;
            } else if (transition == CaseFileItemTransition.Create) {
                if (getLastTransition() == null) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // Remain in this state.
                return true;
            }
        } else // this means (state == CaseFileState.Discarded)
        {
            // Cannot do anything in discarded state
            return false;
        }
    }

    private void makeTransition(CaseFileItemTransition transition) {
        if (!changeState(transition)) {
            throw new TransitionDeniedException("Cannot apply transition " + transition + " on CaseFileItem " + getName() + ", because it is in state " + state);
        }

        // Update our transition
        this.lastTransition = transition;

        CaseFileEvent event = new CaseFileEvent(this.getCaseInstance(), this.getName(), transition, getValue(), getPath(), getState(), indexInArray);
        // Now inform the sentry network of our change
        getCaseInstance().addEvent(event);

        addDebugInfo(() -> "Handling case file item transition " + getName() + "." + transition);

        // Then inform the activating sentries
        connectedEntryCriteria.forEach(onPart -> onPart.inform(this));

        // Finally iterate the terminating sentries and inform them
        connectedExitCriteria.forEach(onPart -> onPart.inform(this));

        addDebugInfo(() -> "Completed case file item transition " + getName() + "." + transition);

        event.finished();
    }

    // TODO: The following 4 methods should generate specific events instyead of generic CaseFileEvent event
    public void createContent(Value<?> newContent) {
        validateState(State.Null, "create");
        validateContents(newContent);
        setValue(newContent);
        makeTransition(CaseFileItemTransition.Create);
    }

    public void replaceContent(Value<?> newContent) {
        validateState(State.Available, "replace");
        validateContents(newContent);
        setValue(newContent);
        makeTransition(CaseFileItemTransition.Replace);
    }

    public void updateContent(Value<?> newContent) {
        validateState(State.Available, "update");
        validateContents(newContent);
        mergeValue(newContent);
        makeTransition(CaseFileItemTransition.Update);
    }

    public void deleteContent() {
        validateState(State.Available, "delete");
        removedValue = value;
        setValue(Value.NULL);
        makeTransition(CaseFileItemTransition.Delete);
        // Now recursively also delete all of our children... Are you sure? Isn't this overinterpreting the spec?
        getChildren().forEach((definition, childItem) -> childItem.deleteContent());
    }

    /**
     * Check whether we are in the correct state to execute the operation
     * @param expectedState
     * @param operationName
     */
    private void validateState(State expectedState, String operationName) {
        if (state != expectedState) {
            throw new TransitionDeniedException("Cannot " + operationName + " case file item " + getName() + " because it is in state " + state + " and should be in state " + expectedState + ".");
        }
    }

    /**
     * Check if the new content matches our definition
     * @param newContent
     */
    private void validateContents(Value<?> newContent) {
        getDefinition().getCaseFileItemDefinition().getDefinitionType().validate(this, newContent);
    }

    /**
     * Sets the value of this CaseFileItem. Internal framework method not to be used from applications.
     * @param newValue
     */
    public void setValue(Value<?> newValue) {
        String type = newValue == null ? "" : newValue.getClass().getName();
        addDebugInfo(DebugEvent.class, e -> e.addMessage("Setting case file item [" + getPath() + "] value", newValue));

        this.value = newValue;
        if (newValue != null) newValue.setOwner(this);

        Value<?> valueToPropagate = newValue;

        if (array != null) { // Update the array we belong too as well
            array.childChanged(this);
            valueToPropagate = array.getValue();
        }
        propagateValueChange(getName(), valueToPropagate);
    }

    /**
     * This updates the json structure of the parent CaseFileItem, without triggering CaseFileItemTransitions
     * @param childName
     * @param childValue
     */
    private void propagateValueChange(String childName, Value<?> childValue) {
        if (parent != null) {
            if (parent.value==null || parent.value == Value.NULL) {
                addDebugInfo(() -> "Creating a location in parent "+parent.getPath()+" to store the newly changed child "+getName());
                parent.value = new ValueMap();
            }
            if (parent.value instanceof ValueMap) {
                ((ValueMap) parent.value).put(childName, childValue);
                // And ... recurse
                parent.propagateValueChange(parent.getName(), parent.value);
            } else {
                addDebugInfo(() -> "Cannot propagate change in " + getPath()+" into parent, because it's value is not a ValueMap but a " + parent.value.getClass().getName());
            }
        }
    }

    private void mergeValue(Value<?> newValue) {
        setValue(value.merge(newValue));
    }

    /**
     * Returns the content of the CaseFileItem
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
     * @return
     */
    public State getState() {
        return state;
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
     * Iterator on "this". Returns <code>this</code> by default. CaseFileItemArray overwrites it and returns it's elements.
     * Sentry uses this method to transparently connect all items with this definition, regardless whether they are
     * hold within an array or directly under the parent. This additionally avoids that the CaseFileItemArray itself gets 
     * connected to the sentry.
     * @return
     */
    public Iterator<CaseFileItem> iterator() {
        final CaseFileItem self = this;
        return new Iterator<CaseFileItem>() {
            boolean hasNext = true;

            @Override
            public CaseFileItem next() {
                hasNext = false;
                return self;
            }

            @Override
            public boolean hasNext() {
                return hasNext;
            }
        };
    }
    
    /**
     * Reference to most recently updated case file item. Returns <code>this</code> by default. CaseFileItemArray overwrites it
     * and returns the most recently changed / updated case file item in it's array
     * @return
     */
    public CaseFileItem getCurrent() {
        return this;
    }

    /**
     * Resolve the path on this case file item. Base implementation that is overridden in {@link CaseFileItemArray}, where the index
     * accessor of the path is used to navigate to the correct case file item.
     * @param currentPath
     * @return
     */
    CaseFileItem resolve(Path currentPath) {
        return this;
    }

    /**
     * Akka helper method, in order to restore case file item state upon recovery. Not to be used by any other application.
     */
    public void recover(CaseFileEvent event) {
        this.state = event.getState();
        this.indexInArray = event.getIndex();
        this.lastTransition = event.getTransition();
        this.value = event.getValue();
        if (this.array != null) { // Update the array we belong too as well
            this.array.childChanged(this);
        }
    }

    /**
     * Returns the location of this case file item within the array it belongs to. If the case file item is not "iterable", then -1 is returned.
     * @return
     */
    public int getIndex() {
        return indexInArray;
    }
}

/**
 * Case file item that represents an empty item.
 * See CMMN 1.0 specification page 107 ("an empty case file item must be returned")
 *
 */
class EmptyCaseFileItem extends CaseFileItem {
    private final static Logger logger = LoggerFactory.getLogger(EmptyCaseFileItem.class);

    EmptyCaseFileItem(CaseFileItem parent, String creationReason) {
        super(parent.getCaseInstance(), parent.getDefinition(), parent);
        logger.warn(creationReason);
    }

    @Override
    public void createContent(Value<?> newContent) {
        logger.warn("Creating content in EmptyCaseFileItem");
    }

    @Override
    public void updateContent(Value<?> newContent) {
        logger.warn("Updating content in EmptyCaseFileItem");
    }

    @Override
    public void replaceContent(Value<?> newContent) {
        logger.warn("Replacing content in EmptyCaseFileItem");
    }

    @Override
    public void deleteContent() {
        logger.warn("Deleting content in EmptyCaseFileItem");
    }
    
    @Override
    public Value<?> getValue() {
        logger.warn("Returning value from EmptyCaseFileItem");
        return Value.NULL;
    }
    
    @Override
    public void setValue(Value<?> newValue) {
        logger.warn("Setting value in EmptyCaseFileItem");
    }

    @Override
    void bindParameter(Parameter<?> p, Value<?> parameterValue) {
        logger.warn("Binding parameter to EmptyCaseFileItem");
    }

    @Override
    public Iterator<CaseFileItem> iterator() {
        logger.warn("Iterating EmptyCaseFileItem");
        return new ArrayList<CaseFileItem>().iterator();
    }
}