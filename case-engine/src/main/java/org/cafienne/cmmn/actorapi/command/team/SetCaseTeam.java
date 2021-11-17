package org.cafienne.cmmn.actorapi.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.CaseUserIdentity;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

import java.io.IOException;

/**
 * Command to set the case team
 */
@Manifest
public class SetCaseTeam extends CaseTeamCommand {

    private final CaseTeam caseTeam;

    public SetCaseTeam(CaseUserIdentity user, String caseInstanceId, CaseTeam caseTeam) {
        super(user, caseInstanceId);
        this.caseTeam = caseTeam;
    }

    public SetCaseTeam(ValueMap json) {
        super(json);
        this.caseTeam = CaseTeam.deserialize(json.with(Fields.team));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.team, caseTeam);
    }

    @Override
    public void validate(Team team) {
        // New team cannot be empty
        if (caseTeam.isEmpty()) throw new CaseTeamError("The new case team cannot be empty");
        // New team also must have owners
        if (caseTeam.owners().isEmpty()) throw new CaseTeamError("The new case team must have owners");
        // New team roles must match the case definition
        caseTeam.validate(team.getDefinition());
    }

    @Override
    protected void process(Team team) {
        team.replace(this.caseTeam);
    }
}
