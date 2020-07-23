package org.cafienne.cmmn.instance.casefile;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.cmmn.instance.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Case file item that represents an empty item.
 * See CMMN 1.0 specification page 107 ("an empty case file item must be returned")
 */
class EmptyCaseFileItem extends CaseFileItem {
    private final static Logger logger = LoggerFactory.getLogger(EmptyCaseFileItem.class);

    EmptyCaseFileItem(CaseFileItem parent, String creationReason) {
        super(parent.getCaseInstance(), parent.getDefinition(), parent);
        logger.warn(creationReason);
    }

    @Override
    protected void adoptContent(Value<?> newContentFromParent) {
        logger.warn("Adopting content in EmptyCaseFileItem");
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
    protected void setValue(Value<?> newValue) {
        logger.warn("Setting value in EmptyCaseFileItem");
    }

    @Override
    public void bindParameter(Parameter<?> p, Value<?> parameterValue) {
        logger.warn("Binding parameter to EmptyCaseFileItem");
    }
}
