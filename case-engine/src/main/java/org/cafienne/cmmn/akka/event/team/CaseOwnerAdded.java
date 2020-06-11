package org.cafienne.cmmn.akka.event.team;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Member;

/**
 * Event caused when a team member has become owner
 */
@Manifest
public class CaseOwnerAdded extends CaseTeamMemberEvent {

    public CaseOwnerAdded(Case caseInstance, Member member) {
        super(caseInstance, member);
    }

    public CaseOwnerAdded(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getMemberDescription()+" is now owner]";
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }
}
