package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

public abstract class Criterion extends CMMNElement<CriterionDefinition> {
    protected final Stage<?> stage;
    protected final Sentry sentry;

    protected Criterion(Stage<?> stage, CriterionDefinition definition) {
        super(stage, definition);
        this.stage = stage;
        this.sentry = new Sentry(stage, this);
    }

    protected abstract void satisfy();

    public boolean isActive() {
        return sentry.isActive();
    }

    public boolean isSatisfied() {
        return sentry.isSatisfied();
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