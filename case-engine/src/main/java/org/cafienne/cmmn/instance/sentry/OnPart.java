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

    protected String getSourceName() {
        return getDefinition().getSourceDefinition().getName();
    }

    abstract Enum<?> getStandardEvent();

    abstract void connectToCase();

    abstract void releaseFromCase();

    public abstract void inform(I item, E event);

    abstract ValueMap toJson();

    abstract void dumpMemoryStateToXML(Element sentryXML, boolean showConnectedPlanItems);
}