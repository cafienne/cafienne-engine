package org.cafienne.cmmn.instance.sentry;

import org.cafienne.cmmn.definition.ItemDefinition;
import org.cafienne.cmmn.definition.XMLElementDefinition;
import org.cafienne.cmmn.definition.sentry.CriterionDefinition;
import org.cafienne.cmmn.instance.PlanItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public abstract class CriteriaListener<T extends CriterionDefinition, C extends Criterion<T>> {
    protected final PlanItem<?> item;
    protected final Collection<C> criteria = new ArrayList<>();
    protected final Collection<T> definitions;
    private final String logDescription;

    protected CriteriaListener(PlanItem<?> item, Collection<T> definitions) {
        this.item = item;
        this.definitions = definitions;
        this.logDescription = getClass().getSimpleName().substring(8).toLowerCase(Locale.ROOT);
    }

    /**
     * Start listening to the sentry network
     */
    public void connect() {
        this.definitions.forEach(this::addCriterion);
        if (!criteria.isEmpty()) {
            item.getCaseInstance().addDebugInfo(() -> "Connected " + item + " to " + criteria.size() + " " + logDescription + " criteria");
        }
    }

    private void addCriterion(T definition) {
        C criterion = createCriterion(definition);
        item.MigDevConsole("Created criterion " + criterion);
        criteria.add(criterion);
    }

    protected abstract C createCriterion(T definition);

    /**
     * Stop listening to the sentry network, typically when the criterion is satisfied.
     */
    public void release() {
        if (!criteria.isEmpty()) {
            item.getCaseInstance().addDebugInfo(() -> "Disconnecting " + item + " from " + criteria.size() + " " + logDescription + " criteria");
        }
        new ArrayList<>(criteria).forEach(this::release);
    }

    /**
     * Removes the criterion from our collection and tells the criterion to disconnect from the network.
     *
     * @param criterion
     */
    private void release(C criterion) {
        criteria.remove(criterion);
        criterion.release();
    }

    public abstract void satisfy(Criterion<?> criterion);

    protected abstract void migrateCriteria(ItemDefinition newItemDefinition);

    protected void migrateCriteria(Collection<T> newDefinitions) {
        item.MigDevConsole(getClass().getSimpleName() + "[" + item.getName() + "]: migrating " + criteria.size() + " criteria to " + newDefinitions.size() + " new definitions");
        Collection<C> existingCriteria = new ArrayList<>(criteria);

        existingCriteria.forEach(criterion -> migrateCriterion(criterion, newDefinitions));
        newDefinitions.stream().filter(this::hasCriterion).forEach(this::addCriterion);
    }

    private boolean hasCriterion(T definition) {
        return this.criteria.stream().noneMatch(c -> c.getDefinition() == definition);
    }

    private void migrateCriterion(C criterion, Collection<T> newDefinitions) {
        T oldDefinition = criterion.getDefinition();
        T newDefinition = XMLElementDefinition.findDefinition(oldDefinition, newDefinitions);
        if (newDefinition != null) {
            item.MigDevConsole("Updating criterion " + criterion);
            criterion.migrateDefinition(newDefinition);
        } else {
            // Not sure what to do here. Remove the criterion?
            // Search for a 'nearby' alternative?
            item.MigDevConsole("Dropping criterion " + criterion);
            this.release(criterion);
        }
    }
}
