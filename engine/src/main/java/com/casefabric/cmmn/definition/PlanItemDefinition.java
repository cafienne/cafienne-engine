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

package com.casefabric.cmmn.definition;

import com.casefabric.cmmn.definition.extension.workflow.FourEyesDefinition;
import com.casefabric.cmmn.definition.extension.workflow.RendezVousDefinition;
import com.casefabric.cmmn.definition.sentry.*;
import com.casefabric.cmmn.instance.casefile.CaseFileItemTransition;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PlanItemDefinition extends CMMNElementDefinition implements ItemDefinition {
    private ItemControlDefinition planItemControl;
    private PlanItemDefinitionDefinition definition;
    private final Collection<EntryCriterionDefinition> entryCriteria = new ArrayList<>();
    private final Collection<ReactivateCriterionDefinition> reactivationCriteria = new ArrayList<>();
    private final Collection<ExitCriterionDefinition> exitCriteria = new ArrayList<>();
    private final String planItemDefinitionRefValue;
    private final FourEyesDefinition fourEyesDefinition;
    private final RendezVousDefinition rendezVousDefinition;

    public PlanItemDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        this.planItemDefinitionRefValue = parseAttribute("definitionRef", true);

        parse("entryCriterion", EntryCriterionDefinition.class, this.entryCriteria);
        parse("exitCriterion", ExitCriterionDefinition.class, this.exitCriteria);
        parseExtension("reactivateCriterion", ReactivateCriterionDefinition.class, this.reactivationCriteria);
        planItemControl = parse("itemControl", ItemControlDefinition.class, false);
        fourEyesDefinition = parseExtension("four_eyes", FourEyesDefinition.class);
        rendezVousDefinition = parseExtension("rendez_vous", RendezVousDefinition.class);
    }

    @Override
    public String toString() {
        return definition.getType() + "['" + getName() + "']";
    }

    @Override
    public ItemControlDefinition getPlanItemControl() {
        return planItemControl;
    }

    @Override
    public PlanItemDefinitionDefinition getPlanItemDefinition() {
        if (definition == null) {
            // Hmmm, apparently resolving is not yet done... (or it really cannot be found, in which case we again go search for it ;))
            this.definition = getCaseDefinition().findPlanItemDefinition(planItemDefinitionRefValue);
        }
        return definition;
    }

    public StageDefinition getStageDefinition() {
        return getParentElement();
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

    public FourEyesDefinition getFourEyesDefinition() {
        return fourEyesDefinition;
    }

    @Override
    public RendezVousDefinition getRendezVousDefinition() {
        return rendezVousDefinition;
    }

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        if (this.getPlanItemDefinition() == null) {
            getCaseDefinition().addReferenceError(getContextDescription() + " refers to a definition named " + planItemDefinitionRefValue + ", but that definition is not found");
            return; // Avoid further checking on this element
        }
        // If the plan item has no name, it has to be taken from the definition
        if (getName().isEmpty()) {
            setName(getPlanItemDefinition().getName());
        }
    }

    @Override
    protected void validateElement() {
        super.validateElement();
        if (planItemControl == null) {
            // Inherit default itemControl from definition if it is not set on the plan item itself.
            planItemControl = this.definition.getDefaultControl();
        }

        // CMMN 1.0 spec page 23 says (yes indeed with a typo ...):
        // A PlanItem that is defined by a Task that is non-blocking (isBlocking set to "false") MUST NOT have exitCreteriaRefs.
        if (getPlanItemDefinition() instanceof TaskDefinition) {
            if (!((TaskDefinition<?>) getPlanItemDefinition()).isBlocking()) {
                if (!this.exitCriteria.isEmpty()) {
                    getCaseDefinition().addDefinitionError(getContextDescription() + " has exit sentries, but these are not allowed for a non blocking task");
                }
            }
        }

        // CMMN 1.1 spec 5.4.11 page 53 says:
        //  A PlanItem that has a PlanItemControl that contains a RepetitionRule, MUST have either an entry criterion
        //   that refers to a Sentry that has at least one OnPart or no entry criteria at all.
        if (!this.planItemControl.getRepetitionRule().isDefault()) {
            if (this.entryCriteria.isEmpty()) {
                // Stages and Tasks are ok without entry criteria. But milestones must have entry criteria if they have a repetition rule
                if (getPlanItemDefinition() instanceof MilestoneDefinition) {
                    getCaseDefinition().addDefinitionError(getContextDescription() + " has a repetition rule defined, but no entry criteria. This is mandatory.");
                }
            } else {
                // Check whether there is at least one entry criterion having an on part.
                if (this.getEntryCriteria().stream().noneMatch(EntryCriterionDefinition::hasOnParts)) {
                    getCaseDefinition().addDefinitionError(getContextDescription() + " has a repetition rule defined, but no entry criteria with at least one on part. This is mandatory.");
                }
            }
        }

        checkTaskPairingConstraints();
    }

    private PlanItemStarter starter = null;

    @Override
    public PlanItemStarter getStarter() {
        if (starter == null) {
            if (getEntryCriteria().isEmpty()) {
                // If we have no entry criteria, we get started when our stage is started.
                //  So let's
                starter = PlanItemStarter.hasNoEntryCriteria(this, getStageDefinition().findItemDefinition().getStarter());
            } else {
                starter = getEntryCriteriaStarters();
            }
        }
        return starter;
    }

    public boolean hasExits() {
        if (getExitCriteria().isEmpty()) {
            CMMNElementDefinition parent = getParentElement();
            if (parent instanceof StageDefinition) {
                return ((StageDefinition) parent).findItemDefinition().hasExits();
            }
            return false;
        } else {
            return true;
        }
    }

    private PlanItemStarter getEntryCriteriaStarters() {
        Optional<PlanItemStarter> immediate = getEntryCriteria().stream().map(this::getEntryCriteriaStarter).filter(PlanItemStarter::isImmediate).findFirst();
        return immediate.orElse(PlanItemStarter.Later(this));
    }

    private PlanItemStarter getEntryCriteriaStarter(EntryCriterionDefinition entryCriterion) {
        Collection<OnPartDefinition> onParts = entryCriterion.getSentryDefinition().getOnParts();
        if (onParts.isEmpty()) {
            return PlanItemStarter.hasNoOnParts(this);
        }
        Collection<PlanItemStarter> onPartStarters = onParts.stream().map(this::getOnPartStarter).collect(Collectors.toList());
        if (onPartStarters.stream().anyMatch(PlanItemStarter::isLater)) {
            // If one part does not start immediately, the whole entry criterion will start immediately ...
            return PlanItemStarter.Later(this);
        } else {
            // Just return the first one ("or else throw" cannot happen, as there must be more than one).
            return onPartStarters.stream().findFirst().orElseThrow();
        }
    }

    private PlanItemStarter getOnPartStarter(OnPartDefinition part) {
        if (part instanceof CaseFileItemOnPartDefinition) {
            // Note: if a CaseFileItemOnPart comes from a CaseInputParameter, then the item
            //  may get created immediately as well. So we do not allow such a scenario, even if the parameter
            //  would be empty upon actual case instantiation.
            if (part.asFile().getStandardEvent() == CaseFileItemTransition.Create) {
                if (getCaseDefinition().getInputParameters().values().stream().anyMatch(parameter -> parameter.getBinding().contains(part.asFile().getSourceDefinition()))) {
                    return PlanItemStarter.hasImmediateCaseFileCreation(this);
                }
            }
        } else {
            // OnPart source. If it is a Milestone or TimerEvent, we have to do deeper investigations,
            //  otherwise this plan item will not be started immediately.
            ItemDefinition source = part.asPlan().getSourceDefinition();
            if (source.getItemType().isMilestone()) {
                return PlanItemStarter.hasImmediateMilestone(this, source.getStarter());
            } else if (source.getItemType().isTimerEvent()) {
                PlanItemStarter sourceStarter = source.getStarter();
                if (sourceStarter.isImmediate() && !source.hasExits()) {
                    return PlanItemStarter.hasImmediateTimer(this, sourceStarter);
                }
            }
        }
        return PlanItemStarter.Later(this);
    }

    @Override
    public String getContextDescription() {
        String type = getPlanItemDefinition() != null ? getPlanItemDefinition().getType() : "Plan item";
        return type + " " + getName();
    }

    @Override
    public boolean equalsWith(Object object) {
        return equalsWith(object, this::samePlanItem);
    }

    public boolean samePlanItem(PlanItemDefinition other) {
        return sameIdentifiers(other)
                && same(this.planItemControl, other.planItemControl)
                && same(this.definition, other.definition)
                && same(this.entryCriteria, other.entryCriteria)
                && same(this.exitCriteria, other.exitCriteria);
    }

    /**
     * Compare method that determines whether the ItemDefinition has the same path as this PlanItemDefinition.
     * <p>
     * The path matches if an element has either the same name or the same id and has the same class,
     * and is at the same depth inside the ModelDefinition, and the parent elements match as well.
     * Note that for some elements, name and id are irrelevant, as there can be only one instance (e.g, PlanItemControl or CaseFileDefinition).
     */
    public boolean samePath(ItemDefinition other) {
        if (this == other) {
//            System.out.println("Comparing ourselves on similarity in " + this.getClass().getSimpleName());
            return true;
        }
        if (other == null) {
            return false;
        }

        if (!other.getPlanItemDefinition().getClass().equals(this.getPlanItemDefinition().getClass())) {
            // Both must have same type (like both must be HumanTask or CaseTask or Milestone, etc.)
            // Note: therefore this does NOT compare whether both are PlanItem or one is Discretionary and the other not.
            return false;
        }
        if (!hasMatchingIdentifier(other)) {
            return false;
        }

//        System.out.println("Determining similarity of " + this.getClass().getSimpleName());

        // Now iterate the stages that we and the other belong to, and check whether they have same identifiers.
        List<StageDefinition> ourStageChain = getStageChain(this);
        List<StageDefinition> otherStageChain = getStageChain(other);
        if (ourStageChain.size() != otherStageChain.size()) {
            return false;
        }
        for (int i = 0; i < ourStageChain.size(); i++) {
            StageDefinition leftStage = ourStageChain.get(i);
            StageDefinition rightStage = otherStageChain.get(i);
            if (!leftStage.hasMatchingIdentifier(rightStage)) {
                return false;
            }
        }
        return true;
    }

    private List<StageDefinition> getStageChain(ItemDefinition item) {
        List<StageDefinition> chain = new ArrayList<>();
        XMLElementDefinition parent = ((XMLElementDefinition) item).getParentElement();
        while (parent != null && !(parent instanceof CasePlanDefinition)) {
            if (parent instanceof StageDefinition) {
                chain.add((StageDefinition) parent);
            }
            parent = parent.getParentElement();
        }
        return chain;
    }
}
