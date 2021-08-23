/*
 * Copyright 2014 - 2019 Cafienne B.V.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.OnPartDefinition;
import org.cafienne.cmmn.instance.CMMNElement;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.casefile.CaseFileItem;
import org.cafienne.json.ValueMap;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class OnPart<T extends OnPartDefinition, E extends StandardEvent<?, ?>, I extends TransitionGenerator<E>> extends CMMNElement<T> {
    protected final Criterion<?> criterion;
    protected Collection<I> connectedItems = new ArrayList<>();

    protected OnPart(Criterion<?> criterion, T definition) {
        super(criterion, definition);
        this.criterion = criterion;
    }

    public Criterion<?> getCriterion() {
        return criterion;
    }

    @Override
    public String toString() {
        String printedItems = connectedItems.isEmpty() ? "No items '" + getSourceName() + "' connected" : connectedItems.stream().map(item -> item.getPath().toString()).collect(Collectors.joining(","));
        return getStandardEvent() + " of " + printedItems;
    }

    /**
     * Used for logging.
     * @return
     */
    protected String getSourceName() {
        return getDefinition().getSourceDefinition().getName();
    }

    abstract Enum<?> getStandardEvent();

    /**
     * Connect this OnPart to relevant items inside the case (based on the source definition of the on part)
     */
    abstract void connectToCase();

    /**
     * Release this OnPart from the case, i.e., inform the source definition related elements to no longer publish the standard events to us.
     */
    abstract void releaseFromCase();

    /**
     * Determine whether this on part wants to listen to transitions in the case file item, and if so, connect to it.
     * @param caseFileItem Item to potentially connect to
     */
    protected void establishPotentialConnection(CaseFileItem caseFileItem) {
        // By default, an empty implementation; Only CaseFileItemOnPart has an implementation.
    }

    /**
     * Determine whether this on part wants to listen to transitions in the plan item, and if so, connect to it.
     * @param planItem Item to potentially connect to
     */
    protected void establishPotentialConnection(PlanItem<?> planItem) {
        // By default, an empty implementation; Only PlanItemOnPart has an implementation.
    }

    public abstract void inform(I item, E event);

    abstract ValueMap toJson();

    abstract void dumpMemoryStateToXML(Element sentryXML, boolean showConnectedPlanItems);
}