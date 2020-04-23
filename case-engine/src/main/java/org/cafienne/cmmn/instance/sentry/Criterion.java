package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

public abstract class Criterion<D extends CriterionDefinition> extends CMMNElement<D> {
    protected final Stage<?> stage;
    protected final Sentry sentry;

    protected Criterion(Stage stage, D definition) {
        super(stage, definition);
        this.stage = stage;
        this.sentry = new Sentry(stage, this);
    }

    protected abstract void satisfy();

    public abstract void addPlanItem(PlanItem planItem);

    public boolean isActive() {
        return sentry.isActive();
    }

    public boolean isSatisfied() {
        return sentry.isSatisfied();
    }

    @Override
    public D getDefinition() {
        return super.getDefinition();
    }

    /**
     * Whether this is an activating or terminating sentry. This information is important for the order in which sentries are triggered.
     *
     * @return
     */
    public abstract boolean isEntryCriterion();

    @Override
    public String toString() {
        return getDefinition().toString();
    }

    public Element dumpMemoryStateToXML(Element parentElement, boolean showConnectedPlanItems) {
        return sentry.dumpMemoryStateToXML(parentElement, showConnectedPlanItems);
    }
}