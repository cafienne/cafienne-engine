package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
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
    private Collection<Criterion> criteria = new ArrayList<>();

    public SentryNetwork(Case caseInstance) {
        this.caseInstance = caseInstance;
    }

    /**
     * Connect a new {@link CaseFileItem} to the network
     * @param item
     */
    public void connect(CaseFileItem item) {
        for (Criterion criterion : criteria) {
            criterion.establishPotentialConnection(item);
        }
    }

    /**
     * Connect a new {@link PlanItem} to the network
     * @param item
     */
    public void connect(PlanItem item) {
        for (Criterion criterion : criteria) {
            criterion.establishPotentialConnection(item);
        }
        item.getEntryCriteria().connect();
        item.getExitCriteria().connect();
    }

    /**
     * Add a criterion to the network
     * @param criterion
     */
    void add(Criterion criterion) {
        criteria.add(criterion);
    }

    /**
     * Remove a criterion from the network
     * @param criterion
     */
    void remove(Criterion criterion) {
        this.criteria.remove(criterion);
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("SentryNetwork has " + criteria.size()+ " criteria:");
        criteria.forEach(c -> string.append("\n\t- "+c));
        return string.toString() + "\n";
    }

    /**
     * Some entry criteria may listen not only to plan items, but also to a specific exit criterion of a plan item.
     * They can retrieve it through this method. Note this method will not create the criterion...
     * @param definition
     * @return
     */
    Criterion findRelatedExitCriterion(PlanItem item, ExitCriterionDefinition definition) {
       for (Criterion criterion : criteria) {
           if (criterion.getDefinition().equals(definition)) {
                if (criterion.getTarget() == item) {
                        return criterion;
                }
           }
        }
        // Pretty weird, not sure what to do here. Probably we need to make it such that when the
        // exit criterion is created, it will also connect to those entry criteria that relate to it.
        return null;
    }
}
