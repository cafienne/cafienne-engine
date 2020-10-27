package org.cafienne.cmmn.instance.casefile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.cafienne.cmmn.definition.Multiplicity;
import org.cafienne.cmmn.definition.casefile.CaseFileError;
import org.cafienne.cmmn.definition.casefile.CaseFileItemDefinition;
import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.cmmn.instance.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Container for case file item with multiplicity {@link Multiplicity#ZeroOrMore} or {@link Multiplicity#OneOrMore}
 *
 */
public class CaseFileItemArray extends CaseFileItem implements List<CaseFileItem> {

    private final static Logger logger = LoggerFactory.getLogger(CaseFileItemArray.class);

    /**
     * CaseFileItemCollection has a field {@link CaseFileItemCollection#getItems()}, holding the children
     * of a CaseFileItem. However, for a CaseFileItemArray, these items may not be accessed, 
     * since the array consists itself of CaseFileItems each having their own children.
     */
    private final List<CaseFileItem> actualArrayItems = new ArrayList();
    /**
     * "Override" member of the private {@link CaseFileItem} value field. Is returned in the override
     * of {@link CaseFileItemArray#getValue()} method
     */
    private final ValueList actualValueOfCaseFileItemArray;

    private CaseFileItem current; // Reference to most recently used/created/changed/updated CaseFileItem within the array.

    public CaseFileItemArray(Case caseInstance, CaseFileItemDefinition definition, CaseFileItemCollection<?> parent) {
        // Invoking the special constructor that skips informing the sentry network. Watch out: the only
        // difference with the normal super's constructor is the order of the first two arguments!
        // This logic is implemented through the iterator() method.
        super(definition, caseInstance, parent);
        actualValueOfCaseFileItemArray = new ValueList();
        actualValueOfCaseFileItemArray.setOwner(this);
    }

    @Override
    protected CaseFileItem getItem(int index) {
        if (index < 0 || index >= actualArrayItems.size()) {

            // Special case here. If recovery is running, then this code is being invoked from CaseFileEvent.recover()
            // In that case, we must not create an EmptyCaseFileItem (which is a kind of NULL value), but a real one instead.
            // However, that is ONLY legal if it is the "next" in line, i.e., it is the next one to be added to the array.

            if (getCaseInstance().recoveryRunning()) {
                if (index == actualArrayItems.size()) {
                    // This will automatically update 'current' to last one that is being recovered as it happens in sequence
                    current = getNextItem();
                    return current;
                }
                if (index > actualArrayItems.size()) {
                    // This is quite a strange situation. Let's log it to the error console.
                    String errorMsg = "Encountering an strange situation during recovery. " + "Need to recover case file item " + this.getPath() + " with index " + index + ", but the current "
                            + "array only holds " + actualArrayItems.size() + " items, so we must have missed few recovery events. "
                            + "Recovery continues with an empty CaseFileItem, holding illegal values";
                    logger.error(errorMsg);
                }
            }
            return new EmptyCaseFileItem(this, "Index " + index + " is out of bounds in CaseFileItem " + this.getPath());
        }

        return actualArrayItems.get(index);
    }

    @Override
    public Value<?> getValue() {
        return actualValueOfCaseFileItemArray;
    }

    /**
     * Framework method to indicate the change of an case file item inside our array
     * @param item
     */
    @Override
    protected void itemChanged(CaseFileItem item) {
        // Update the reference to current;
        current = item;

        // Now lookup the item in our ValueList and update the element.
        Value<?> itemContent = item.getValue();
        int itemIndex = item.getIndex();
        if (itemIndex < actualValueOfCaseFileItemArray.size()) {
            actualValueOfCaseFileItemArray.set(itemIndex, itemContent);
        } else if (itemIndex == actualValueOfCaseFileItemArray.size()) {
            // It's the latest and greatest new item
            actualValueOfCaseFileItemArray.add(itemContent);
        } else {
            throw new CaseFileError("We're not letting you in, number " + itemIndex + ", because we only have " + actualValueOfCaseFileItemArray.size() + " items, and it seems you're skipping one or more");
        }
    }

    protected boolean allowTransition(CaseFileItemTransition intendedTransition) {
        // In CaseFileItemArray it is allowed to add new items to the existing array (through Create method)
        if (this.getState() == State.Available && intendedTransition == CaseFileItemTransition.Create) return true;
        return super.allowTransition(intendedTransition);
    }

    private CaseFileItem getNextItem() {
        CaseFileItem nextItem = new CaseFileItem(this, actualArrayItems.size());
        actualArrayItems.add(nextItem);
        return nextItem;
    }

    private void createNewItem(Value value) {
        setState(State.Available);
        getNextItem().createContent(value);
    }

    @Override
    public void createContent(Value<?> newContent) {
        ValueList valueList = newContent.isList() ? (ValueList) newContent : new ValueList(newContent);
        for (Value<?> newChildValue : valueList) {
            createNewItem(newChildValue);
        }
    }

    @Override
    public void updateContent(Value<?> newContent) {
        if (! newContent.isList()) {
            if (current != null) {
                addDebugInfo(() -> "Update for CaseFileItem[" + getPath()+"] with an object structure is executed on 'current':  (" + current.getPath() +")");
                current.updateContent(newContent);
            } else {
                // Plain behavior; 'Replace' on empty array results in Create, whether the element is presented in a list or not
                createNewItem(newContent);
            }
            return;
        }
        ValueList valueList = (ValueList) newContent;
        int numberToUpdate = valueList.size();
        for (int i = 0; i<numberToUpdate; i++) {
            Value<?> newChildValue = valueList.get(i);
            if (actualArrayItems.size() > i) {
                CaseFileItem arrayItem = actualArrayItems.get(i);
                arrayItem.updateContent(newChildValue);
            } else {
                createNewItem(newChildValue);
            }
        }
    }

    @Override
    public void replaceContent(Value<?> newContent) {
        if (! newContent.isList()) {
            if (current != null) {
                addDebugInfo(() -> "Replace for CaseFileItem[" + getPath()+"] with an object structure is executed on 'current':  (" + current.getPath() +")");
                current.replaceContent(newContent);
            } else {
                // Plain behavior; 'Replace' on empty array results in Create, whether the element is presented in a list or not
                createNewItem(newContent);
            }
            return;
        }
        ValueList valueList = (ValueList) newContent;
        int numberToReplace = valueList.size();
        int numberToRemove = actualArrayItems.size() - numberToReplace;
        for (int i = 0; i<numberToReplace; i++) {
            Value<?> newChildValue = valueList.get(i);
            if (actualArrayItems.size() > i) {
                CaseFileItem arrayItem = actualArrayItems.get(i);
                arrayItem.replaceContent(newChildValue);
            } else {
                createNewItem(newChildValue);
            }
        }
        while (numberToRemove-- > 0) {
            // Now clean up the items that "vanished" while replacing
            actualArrayItems.remove(actualArrayItems.size() - 1).itemVanished();
        }
    }

    @Override
    public void deleteContent() {
        setState(State.Discarded);
        // Delete content in reverse order
        int numberToRemove = actualArrayItems.size();
        while (--numberToRemove >= 0) {
            actualArrayItems.get(numberToRemove).deleteContent();
        }
    }

    @Override
    public void dumpMemoryStateToXML(Element parentElement) {
        // Only dump our actual items.
        for (CaseFileItem caseFileItem : actualArrayItems) {
            caseFileItem.dumpMemoryStateToXML(parentElement);
        }
    }

    @Override
    public CaseFileItem getItem(String propertyName) {
        // TODO: can we get rid of this override?
        throw new TransitionDeniedException("Cannot access a property '" + propertyName + "' of the case file item container. Have to address an individual item");
    }

    @Override
    public Iterator<CaseFileItem> iterator() {
        // Note, this overrides both in CaseFileItem as well as in the List interface
        return actualArrayItems.iterator();
    }

    @Override
    public CaseFileItem getCurrent() {
        // Override from CaseFileItem.current(); returns the most recently changed case file item.
        if (current == null) {
            current = getItem(-1); // Get -1 will return EmptyCaseFileItem
        }
        return current;
    }

    @Override
    public boolean add(CaseFileItem e) {
        // TODO Auto-generated method stub
        return actualArrayItems.add(e);
    }

    @Override
    public void add(int index, CaseFileItem element) {
        actualArrayItems.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends CaseFileItem> c) {
        return actualArrayItems.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends CaseFileItem> c) {
        return actualArrayItems.addAll(index, c);
    }

    @Override
    public void clear() {
        actualArrayItems.clear();
    }

    @Override
    public boolean contains(Object o) {
        return actualArrayItems.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // TODO Auto-generated method stub
        return actualArrayItems.containsAll(c);
    }

    @Override
    public CaseFileItem get(int index) {
        return actualArrayItems.get(index);
    }

    @Override
    public int indexOf(Object o) {
        return actualArrayItems.indexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return actualArrayItems.isEmpty();
    }

    @Override
    public int lastIndexOf(Object o) {
        return actualArrayItems.lastIndexOf(o);
    }

    @Override
    public ListIterator<CaseFileItem> listIterator() {
        return actualArrayItems.listIterator();
    }

    @Override
    public ListIterator<CaseFileItem> listIterator(int index) {
        return actualArrayItems.listIterator(index);
    }

    @Override
    public boolean remove(Object o) {
        return actualArrayItems.remove(o);
    }

    @Override
    public CaseFileItem remove(int index) {
        return actualArrayItems.remove(index);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return actualArrayItems.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return actualArrayItems.retainAll(c);
    }

    @Override
    public CaseFileItem set(int index, CaseFileItem element) {
        return actualArrayItems.set(index, element);
    }

    @Override
    public int size() {
        return actualArrayItems.size();
    }

    @Override
    public List<CaseFileItem> subList(int fromIndex, int toIndex) {
        return actualArrayItems.subList(fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return actualArrayItems.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return actualArrayItems.toArray(a);
    }
}
