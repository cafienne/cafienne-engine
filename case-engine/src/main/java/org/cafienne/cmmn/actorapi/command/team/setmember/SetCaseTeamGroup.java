package org.cafienne.cmmn.actorapi.command.team.setmember;

import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.actorapi.command.team.CaseTeamGroup;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

/**
 * Command to add a consent group to the case team
 */
@Manifest
public class SetCaseTeamGroup extends SetCaseTeamMemberCommand<CaseTeamGroup> {
    public SetCaseTeamGroup(CaseUserIdentity user, String caseInstanceId, CaseTeamGroup newMember) {
        super(user, caseInstanceId, newMember);
    }

    public SetCaseTeamGroup(ValueMap json) {
        super(json, CaseTeamGroup::deserialize);
    }

    @Override
    public void validate(Team team) {
        super.validate(team);

        // TODO: this needs furhter implementation


        // Check whether the roles are valid
        newMember.validateRolesExist(team.getDefinition());
    }

    @Override
    protected void process(Team team) {
        team.setGroup(newMember);
    }
}
