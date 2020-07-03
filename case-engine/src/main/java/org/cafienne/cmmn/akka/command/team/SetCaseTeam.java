package org.cafienne.cmmn.akka.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.akka.actor.serialization.Fields;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.cmmn.instance.team.Team;

import java.io.IOException;

/**
 * Command to set the case team
 * CaseTeam must be a json structure
 * <pre>
 * {
 *   "members" : [
 *      {
 *        "user"  : "user1 identifier",
 *        "roles" : [ "rolename1", 
 *                    "rolename2", 
 *                     ... ]
 *      }, 
 *      {
 *        "user"  : "user2 identifier",
 *        "roles" : [ "rolename1", 
 *                    "rolename3", 
 *                     ... ]
 *      }]
 * }
 * </pre>
 *
 * The roles are matched to roles as defined in the case definition.
 * Furthermore validation on the roles is done.
 *
 */
@Manifest
public class SetCaseTeam extends CaseCommand {

    private final CaseTeam newCaseTeam;

    public SetCaseTeam(TenantUser tenantUser, String caseInstanceId, CaseTeam newCaseTeam) {
        // TODO: determine how to do authorization on this command.
        super(tenantUser, caseInstanceId);
        this.newCaseTeam = newCaseTeam;
    }

    public SetCaseTeam(ValueMap json) {
        super(json);
        this.newCaseTeam = CaseTeam.deserialize(json.withArray(Fields.team));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.team, newCaseTeam.getMembers());
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        // New team cannot be empty
        if (newCaseTeam.members().isEmpty()) throw new CaseTeamError("The new case team cannot be empty");
        // New team also must have owners
        if (newCaseTeam.owners().isEmpty()) throw new CaseTeamError("The new case team must have owners");
        // New team roles must match the case definition
        newCaseTeam.validate(caseInstance.getDefinition());
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        Team caseTeam = caseInstance.getCaseTeam();
        caseTeam.clear();
        caseTeam.fillFrom(newCaseTeam);
        return new CaseResponse(this);
    }

}
