package org.cafienne.cmmn.actorapi.command.team;

import org.cafienne.actormodel.exception.AuthorizationException;
import org.cafienne.actormodel.exception.InvalidCommandException;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.actormodel.response.ModelResponse;
import org.cafienne.cmmn.actorapi.command.CaseCommand;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.json.ValueMap;

/**
 * Generic abstraction for commands relating to CaseTeam.
 */
public abstract class CaseTeamCommand extends CaseCommand {
    protected CaseTeamCommand(CaseUserIdentity user, String caseInstanceId) {
        super(user, caseInstanceId);
    }

    protected CaseTeamCommand(ValueMap json) {
        super(json);
    }

    @Override
    public void validate(Case caseInstance) throws InvalidCommandException {
        super.validate(caseInstance);
        if (! caseInstance.getCurrentTeamMember().isOwner()) {
            throw new AuthorizationException("Only case team owners can perform this action");
        }
        validate(caseInstance.getCaseTeam());
    }

    protected abstract void validate(Team team) throws InvalidCommandException;

    @Override
    public ModelResponse process(Case caseInstance) {
        process(caseInstance.getCaseTeam());
        return new CaseResponse(this);
    }

    protected abstract void process(Team team);
}
