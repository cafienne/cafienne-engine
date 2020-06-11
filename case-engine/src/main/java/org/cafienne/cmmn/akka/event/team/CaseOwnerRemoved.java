package org.cafienne.cmmn.akka.event.team;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;

/**
 * Event caused when a team member is no longer owner
 */
@Manifest
public class CaseOwnerRemoved extends CaseTeamMemberEvent {

    public CaseOwnerRemoved(Case caseInstance, MemberKey member) {
        super(caseInstance, member);
    }

    public CaseOwnerRemoved(ValueMap json) {
        super(json);
    }

    @Override
    public String getDescription() {
        return getClass().getSimpleName() + "[" + getMemberDescription()+" is no longer owner]";
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }
}
