package org.cafienne.cmmn.actorapi.event.team;

import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Event caused when a team member has become owner
 */
@Manifest
public class CaseOwnerAdded extends CaseTeamMemberEvent {

    public CaseOwnerAdded(Case caseInstance, MemberKey member) {
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
