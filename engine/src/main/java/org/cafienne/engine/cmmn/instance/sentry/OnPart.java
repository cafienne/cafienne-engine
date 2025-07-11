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

package org.cafienne.engine.cmmn.instance.sentry;

import org.cafienne.engine.cmmn.definition.sentry.OnPartDefinition;
import org.cafienne.engine.cmmn.instance.CMMNElement;
import org.cafienne.engine.cmmn.instance.PlanItem;
import org.cafienne.engine.cmmn.instance.casefile.CaseFileItem;
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
        String printedItems = connectedItems.isEmpty() ? "" : ": [" + connectedItems.stream().map(item -> item.getPath().getPart()).collect(Collectors.joining(",")) + "]";
        return getStandardEvent() + " of " + getSourceType() + "[" + getSourceName() + "] - connected to " + connectedItems.size() + " items" + printedItems;
    }

    /**
     * Used for logging.
     */
    protected String getSourceName() {
        return getDefinition().getSourceDefinition().getName();
    }

    protected String getSourceType() {
        return getDefinition().getSourceDefinition().getType();
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
     *
     * @param caseFileItem Item to potentially connect to
     */
    protected void establishPotentialConnection(CaseFileItem caseFileItem) {
        // By default, an empty implementation; Only CaseFileItemOnPart has an implementation.
    }

    /**
     * Disconnect the item from this on part, as the item is no longer part of the sentry network
     */
    protected void removeConnection(CaseFileItem caseFileItem) {
    }

    /**
     * Determine whether this on part wants to listen to transitions in the plan item, and if so, connect to it.
     *
     * @param planItem Item to potentially connect to
     */
    protected void establishPotentialConnection(PlanItem<?> planItem) {
        // By default, an empty implementation; Only PlanItemOnPart has an implementation.
    }

    /**
     * Disconnect the item from this on part, as the item is no longer part of the sentry network
     */
    protected void removeConnection(PlanItem<?> planItem) {
    }

    public abstract void inform(I item, E event);

    abstract ValueMap toJson();

    abstract void dumpMemoryStateToXML(Element sentryXML, boolean showConnectedPlanItems);
}