package org.cafienne.cmmn.akka.event.team;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.akka.actor.serialization.json.ValueMap;

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
