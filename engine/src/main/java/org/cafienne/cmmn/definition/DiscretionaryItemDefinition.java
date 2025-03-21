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

package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.extension.workflow.FourEyesDefinition;
import org.cafienne.cmmn.definition.extension.workflow.RendezVousDefinition;
import org.cafienne.cmmn.definition.sentry.EntryCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ExitCriterionDefinition;
import org.cafienne.cmmn.definition.sentry.ReactivateCriterionDefinition;
import org.cafienne.cmmn.instance.DiscretionaryItem;
import org.cafienne.cmmn.instance.PlanItem;
import org.cafienne.cmmn.instance.Stage;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;

public class DiscretionaryItemDefinition extends TableItemDefinition implements ItemDefinition {
    private ItemControlDefinition planItemControl;
    private PlanItemDefinitionDefinition definition;
    private final Collection<EntryCriterionDefinition> entryCriteria = new ArrayList<>();
    private final Collection<ReactivateCriterionDefinition> reactivationCriteria = new ArrayList<>();
    private final Collection<ExitCriterionDefinition> exitCriteria = new ArrayList<>();
    private final FourEyesDefinition fourEyesDefinition;
    private final RendezVousDefinition rendezVousDefinition;
    private final String planItemDefinitionRefValue;

    public DiscretionaryItemDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        if (this.getName().isEmpty()) {
            modelDefinition.addDefinitionError("The discretionary item with id " + this.getId() +" must have a name");
        }
        this.planItemDefinitionRefValue = parseAttribute("definitionRef", true);

        parse("entryCriterion", EntryCriterionDefinition.class, this.entryCriteria);
        parse("exitCriterion", ExitCriterionDefinition.class, this.exitCriteria);
        parseExtension("reactivateCriterion", ReactivateCriterionDefinition.class, this.reactivationCriteria);

        planItemControl = parse("itemControl", ItemControlDefinition.class, false);
        fourEyesDefinition = parseExtension("four_eyes", FourEyesDefinition.class);
        rendezVousDefinition = parseExtension("rendez_vous", RendezVousDefinition.class);

        // CMMN 1.0 spec page 32:
        // A DiscretionaryItem that is defined by a Task that is non-blocking (isBlocking set to "false") MUST NOT have exitCreteriaRefs.
        if (this.definition instanceof TaskDefinition) {
            if (!((TaskDefinition<?>) this.definition).isBlocking()) {
                if (!this.exitCriteria.isEmpty()) {
                    getCaseDefinition().addDefinitionError("The plan item " + getName() + " has exit sentries, but these are not allowed for a non blocking task");
                    return;
                }
            }
        }
    }

    @Override
    public ItemControlDefinition getPlanItemControl() {
        return planItemControl;
    }

    @Override
    public PlanItemDefinitionDefinition getPlanItemDefinition() {
        return definition;
    }

    @Override
    public String getType() {
        return definition.getType();
    }

    @Override
    public Collection<EntryCriterionDefinition> getEntryCriteria() {
        return entryCriteria;
    }

    @Override
    public Collection<ReactivateCriterionDefinition> getReactivatingCriteria() {
        return reactivationCriteria;
    }

    @Override
    public Collection<ExitCriterionDefinition> getExitCriteria() {
        return exitCriteria;
    }

    @Override
    public FourEyesDefinition getFourEyesDefinition() {
        return fourEyesDefinition;
    }

    @Override
    public RendezVousDefinition getRendezVousDefinition() {
        return rendezVousDefinition;
    }

    @Override
    public boolean isDiscretionary() {
        return true;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        this.definition = getCaseDefinition().findPlanItemDefinition(planItemDefinitionRefValue);
        if (this.definition == null) {
            getCaseDefinition().addReferenceError("The discretionary item '" + getName() + "' refers to a definition named '" + planItemDefinitionRefValue + "', but that definition is not found");
            return;
        }
        // If the discretionary item has no name, it has to be taken from the definition
        if (getName().isEmpty()) {
            setName(definition.getName());
        }
    }

    @Override
    protected void validateElement() {
        super.validateElement();
        if (planItemControl == null) {
            // Create a default ItemControl
            planItemControl = this.definition.getDefaultControl();
        }

        checkTaskPairingConstraints();
    }

    /**
     * Calculates whether the discretionary item is currently applicable within the context of
     * the containing plan item. Note: the containing plan item must be of type Stage or HumanTask.
     *
     * @param containingPlanItem
     * @return
     */
    public boolean isApplicable(PlanItem<?> containingPlanItem) {
        if (isAlreadyPlanned(containingPlanItem)) {
            return false;
        }
        if (getApplicabilityRules().isEmpty()) {
            containingPlanItem.getCaseInstance().addDebugInfo(() -> this + ": item is applicable because rules are not defined");
            return true;
        } else {
            containingPlanItem.getCaseInstance().addDebugInfo(() -> this + ": checking " + getApplicabilityRules().size() + " applicability rule(s)");
            for (ApplicabilityRuleDefinition rule : getApplicabilityRules()) {
                // If any of the rules evaluates to false, the discretionary item is not allowed
                if (!rule.evaluate(containingPlanItem, rule, this)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Checks if the discretionary item is already planned
     *
     * @param containingPlanItem
     * @return
     */
    private boolean isAlreadyPlanned(PlanItem<?> containingPlanItem) {
        // Go through all plan items in the containing stage, check if there is one with our name
        // and then check if it is not repeating. If not, then there is one, and we cannot add more, so then we are "already planned".
        Stage<?> containingStage = containingPlanItem instanceof Stage<?> ? (Stage<?>) containingPlanItem : containingPlanItem.getStage();
        Collection<PlanItem<?>> currentPlanItemsInStage = containingStage.getPlanItems();
        for (PlanItem<?> planItem : currentPlanItemsInStage) {
            if (planItem.getName().equals(this.getName())) {
                if (!planItem.repeats()) {
                    return true;
                }
            }
        }
        // Not yet planned.
        return false;
    }

    @Override
    public Element dumpMemoryStateToXML(Element parentElement, Stage<?> stage) {
        Element discretionaryXML = parentElement.getOwnerDocument().createElement("discretionaryItem");

        discretionaryXML.setAttribute("name", getName());
        // System.out.println("Dumping memory state for table item "+getName()+", having "+getApplicabilityRules().size()+" rules");
//        discretionaryXML.setAttribute("applicable", "" + isApplicable(stage.getPlanItem()));

        // Also print the roles.
        super.dumpMemoryStateToXML(discretionaryXML, stage);
        parentElement.appendChild(discretionaryXML);

        return discretionaryXML;
    }

    @Override
    public void evaluate(PlanItem<?> containingPlanItem, Collection<DiscretionaryItem> items) {
        if (isApplicable(containingPlanItem)) {
            items.add(createInstance(containingPlanItem));
        }
    }

    /**
     * Create a new instance of this definition inside the specified parent.
     *
     * @param parent The stage or task in which the discretionary item can be planned
     * @return
     */
    public DiscretionaryItem createInstance(PlanItem<?> parent) {
        return new DiscretionaryItem(parent, this);
    }

    @Override
    protected DiscretionaryItemDefinition getDiscretionaryItem(String identifier) {
        if (this.hasIdentifier(identifier)) {
            return this; // We're the one.
        }
        return null;
    }

    @Override
    public String toString() {
        if (definition == null) return super.toString();
        return "DiscretionaryItem[" + definition.getType() + " '" + getName() + "']";
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::sameDiscretionaryItem);
    }

    public boolean sameDiscretionaryItem(DiscretionaryItemDefinition other) {
        return sameTableItem(other)
                && same(definition, other.definition)
                && same(planItemControl, other.planItemControl)
                && same(entryCriteria, other.entryCriteria)
                && same(exitCriteria, other.exitCriteria);
    }
}
