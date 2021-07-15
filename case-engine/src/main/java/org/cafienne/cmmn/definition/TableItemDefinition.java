package org.cafienne.cmmn.definition;

import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.*;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

abstract class TableItemDefinition extends CMMNElementDefinition {
    private final String applicabilityRuleRefs;
    private final String authorizedRoleRefs;
    private final List<ApplicabilityRuleDefinition> applicabilityRules = new ArrayList<>();
    private final Collection<CaseRoleDefinition> authorizedRoles = new ArrayList<>();

    protected TableItemDefinition(Element element, ModelDefinition modelDefinition, CMMNElementDefinition parentElement) {
        super(element, modelDefinition, parentElement);
        applicabilityRuleRefs = parseAttribute("applicabilityRuleRefs", false, "");
        authorizedRoleRefs = parseAttribute("authorizedRoleRefs", false, "");
    }

    /**
     * Fetch a discretionary item from this table item or one of it's children
     *
     * @param identifier
     * @return
     */
    protected abstract DiscretionaryItemDefinition getDiscretionaryItem(String identifier);

    @Override
    protected void resolveReferences() {
        super.resolveReferences();
        StringTokenizer st = new StringTokenizer(applicabilityRuleRefs, " ");
        while (st.hasMoreTokens()) {
            String ruleRef = st.nextToken();
            ApplicabilityRuleDefinition rule = getTable().getApplicabilityRule(ruleRef);
            if (rule == null) {
                getCaseDefinition().addReferenceError("An applicability rule with name " + ruleRef + " is referenced from table item " + getName() + ", but it cannot be found in the planning table");
            } else {
                applicabilityRules.add(rule);
            }
        }

        getCaseDefinition().getCaseTeamModel().resolveRoleReferences(authorizedRoleRefs, authorizedRoles, "Table item " + this);
    }

    /**
     * Returns the first parent planning table of this table item
     *
     * @return
     */
    private PlanningTableDefinition getTable() {
        CMMNElementDefinition ancestor = getParentElement();
        while (ancestor != null) {
            if (ancestor instanceof PlanningTableDefinition) {
                return (PlanningTableDefinition) ancestor;
            }
            ancestor = ancestor.getParentElement();
        }
        return null;
    }

    /**
     * Returns the applicability rules associated with this table item.
     *
     * @return
     */
    protected List<ApplicabilityRuleDefinition> getApplicabilityRules() {
        return applicabilityRules;
    }

    /**
     * Returns the collection of case roles that are allowed to plan this table item
     *
     * @return
     */
    public Collection<CaseRoleDefinition> getAuthorizedRoles() {
        return authorizedRoles;
    }

    /**
     * Determines based on the type of plan item whether it is in a state that is eligible for planning
     *
     * @param planItem
     * @return
     */
    public boolean isPlanningAllowed(PlanItem<?> planItem) {
        // Refactoring thought: this code could also be placed in PlanItemDefinitionInstance, with specific overriding in CasePlan, Stage and HumanTask;
        // however, the algorithm is described in a single place in the specification, section 7.7 on page 80.
        State planItemState = planItem.getState();
        if (planItem instanceof CasePlan) {
            return planItemState == State.Active || planItemState == State.Failed || planItemState == State.Suspended || planItemState == State.Completed || planItemState == State.Terminated;
        } else if (planItem instanceof Stage) {
            return planItemState == State.Active || planItemState == State.Available || planItemState == State.Enabled || planItemState == State.Disabled || planItemState == State.Failed
                    || planItemState == State.Suspended;
        } else { // Must be a HumanTask
            return planItemState == State.Active;
        }
    }

    /**
     * Evaluates the applicability on this table item within the context of the containing plan item (i.e., the stage or task in which the item could be plannable)
     *
     * @param containingPlanItem
     * @param items
     */
    public abstract void evaluate(PlanItem<?> containingPlanItem, Collection<DiscretionaryItem> items);

    public Element dumpMemoryStateToXML(Element tableItemXML, Stage<?> stage) {
        Collection<CaseRoleDefinition> roles = getAuthorizedRoles();
        for (CaseRoleDefinition role : roles) {
            String roleName = role.getName();
            Element roleElement = tableItemXML.getOwnerDocument().createElement("Role");
            tableItemXML.appendChild(roleElement);
            roleElement.setAttribute("name", roleName);
        }

        return tableItemXML;
    }

    protected boolean sameTableItem(TableItemDefinition other) {
        return sameIdentifiers(other)
                && same(applicabilityRules, other.applicabilityRules)
                && same(authorizedRoles, other.authorizedRoles);
    }
}
