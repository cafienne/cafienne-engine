package org.cafienne.cmmn.actorapi.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.actormodel.identity.TenantUser;
import org.cafienne.cmmn.actorapi.response.CaseResponse;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.CaseTeamError;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.infrastructure.serialization.Manifest;
import org.cafienne.json.ValueMap;

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
public class SetCaseTeam extends CaseTeamCommand {

    private final CaseTeam caseTeam;

    public SetCaseTeam(TenantUser tenantUser, String caseInstanceId, CaseTeam caseTeam) {
        // TODO: determine how to do authorization on this command.
        super(tenantUser, caseInstanceId);
        this.caseTeam = caseTeam;
    }

    public SetCaseTeam(ValueMap json) {
        super(json);
        this.caseTeam = CaseTeam.deserialize(json.withArray(Fields.team));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeListField(generator, Fields.team, caseTeam.getMembers());
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        // New team cannot be empty
        if (caseTeam.members().isEmpty()) throw new CaseTeamError("The new case team cannot be empty");
        // New team also must have owners
        if (caseTeam.owners().isEmpty()) throw new CaseTeamError("The new case team must have owners");
        // New team roles must match the case definition
        caseTeam.validate(caseInstance.getDefinition());
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        caseInstance.getCaseTeam().replace(this.caseTeam);
        return new CaseResponse(this);
    }

}
