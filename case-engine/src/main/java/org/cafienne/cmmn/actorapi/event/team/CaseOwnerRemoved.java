package org.cafienne.cmmn.actorapi.event.team;

import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;

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
