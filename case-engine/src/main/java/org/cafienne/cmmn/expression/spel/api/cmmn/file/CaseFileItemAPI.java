package org.cafienne.cmmn.expression.spel.api.cmmn.file;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;

/**
 */
public class CaseFileItemAPI extends APIObject<Case> {
    private final CaseFileItem item;

    public CaseFileItemAPI(CaseFileItem item) {
        super(item.getCaseInstance());
        this.item = item;
        addPropertyReader("index", () -> item.getIndex());
        addPropertyReader("value", this::getValue);
        addPropertyReader("current", () -> new CaseFileItemAPI(item.getCurrent()));
    }

    public Object getValue() {
        // Note: valueAPI.getValue may return the actual primitive value itself.
        return new ValueAPI(item).getValue();
    }
}
