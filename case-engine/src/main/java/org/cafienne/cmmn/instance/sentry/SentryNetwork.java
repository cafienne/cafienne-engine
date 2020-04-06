package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.CaseFileItem;
import org.cafienne.cmmn.instance.PlanItem;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Wrapper class for the collection of all sentries inside the case instance.
 */
public class SentryNetwork {
    private final Case caseInstance;
    /**
     * List of sentries active within the case.
     */
    private Collection<Sentry> sentries = new ArrayList<>();

    public SentryNetwork(Case caseInstance) {
        this.caseInstance = caseInstance;
    }

    /**
     * Connect a new {@link CaseFileItem} to the network
     * @param item
     */
    public void connect(CaseFileItem item) {
        for (Sentry sentry : sentries) {
            sentry.establishPotentialConnection(item);
        }
    }

    /**
     * Connect a new {@link PlanItem} to the network
     * @param item
     */
    public void connect(PlanItem item) {
        for (Sentry sentry : sentries) {
            sentry.establishPotentialConnection(item);
        }
    }

    /**
     * Add a sentry to the network
     * @param sentry
     */
    void add(Sentry sentry) {
        sentries.add(sentry);
    }
}
