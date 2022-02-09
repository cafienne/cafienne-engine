package org.cafienne.cmmn.expression.spel.api.cmmn.file;

import org.cafienne.cmmn.expression.InvalidExpressionException;
import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItemArray;

/**
 */
public class CaseFileItemAPI extends APIObject<Case> {
    protected final CaseFileItem item;
    protected final CaseFileItemAPI parent;

    public CaseFileItemAPI(CaseFileItem item) {
        super(item.getCaseInstance());
        this.item = item;
        parent = item.getParent() == null ? null : new CaseFileItemAPI(item.getParent());
        addPropertyReader("index", () -> item.getIndex());
        addPropertyReader("value", this::getValue);
        addPropertyReader("container", () -> new CaseFileItemAPI(item.getContainer()));
        addPropertyReader("current", () -> new CaseFileItemAPI(item.getCurrent()));
        addPropertyReader("parent", () -> parent);
    }

    public Object get(int index) {
        if (item.getContainer().isArray()) {
            CaseFileItemArray array = item.getContainer();
            return new CaseFileItemAPI(array.get(index)).getValue();
        } else {
            throw new InvalidExpressionException("Cannot read index " + index + " from non-array case file item " + this);
        }
    }

    public Object getValue() {
        // Note: valueAPI.getValue may return the actual primitive value itself.
        return new ValueAPI(item).getValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +"[" + item.getPath() + "]";
    }
}
