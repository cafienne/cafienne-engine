package org.cafienne.cmmn.instance.sentry;

import org.cafienne.akka.actor.serialization.json.Value;
import org.cafienne.akka.actor.serialization.json.ValueList;
import org.cafienne.akka.actor.serialization.json.ValueMap;
import org.cafienne.cmmn.instance.PlanItem;

import java.util.ArrayList;
import java.util.Collection;

public abstract class CriteriaListener<C extends Criterion> {
    protected final PlanItem item;
    protected final Collection<C> criteria = new ArrayList();

    protected CriteriaListener(PlanItem item) {
        this.item = item;
    }

    public abstract void satisfy(C criterion);

    public Value<?> getStateAsValueMap() {
        ValueList state = new ValueList();
        criteria.forEach(c -> state.add(c.getStateAsValueMap()));
        return state;
    }
}
