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
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.w3c.dom.Element;

public abstract class OnPart<T extends OnPartDefinition, I extends CMMNElement<?>> extends CMMNElement<T> {
    protected final Sentry sentry;

    protected OnPart(Sentry sentry, T definition) {
        super(sentry, definition);
        this.sentry = sentry;
    }

    /**
     * Returns the sentry to which this on part belongs.
     *
     * @return
     */
    public Sentry getSentry() {
        return sentry;
    }

    /**
     * Connect the on part to a case file item or plan item
     *
     * @param item
     */
    abstract void connect(I item);

    /**
     * Inform the on part about activity inside I
     *
     * @param item
     */
    public abstract void inform(I item);

    abstract ValueMap toJson();

    abstract Element dumpMemoryStateToXML(Element sentryXML, boolean showConnectedPlanItems);
}