package org.cafienne.cmmn.akka.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.CaseCommand;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Member;
import org.cafienne.cmmn.instance.team.Team;

import java.io.IOException;

/**
 * Command to add a member to the case team
 * Member must be a json structure
 * <pre>
 *   {
 *     "user"  : "user identifier",
 *     "roles" : [ "rolename1", 
 *                 "rolename2", 
 *                  ... ]
 *   }
 * </pre>
 * 
 * The roles are matched to roles as defined in the case definition.
 * Furthermore validation on the roles is done.
 *
 */
@Manifest
public class PutTeamMember extends CaseCommand {
    private final CaseTeamMember jsonNewMember;
    private Member newCaseTeamMember;

    private enum Fields {
        member
    }

    public PutTeamMember(TenantUser tenantUser, String caseInstanceId, CaseTeamMember newMember) {
        // TODO: determine how to do authorization on this command.
        super(tenantUser, caseInstanceId);
        this.jsonNewMember = newMember;
    }

    public PutTeamMember(ValueMap json) {
        super(json);
        jsonNewMember = new CaseTeamMember(readMap(json, Fields.member));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.member, jsonNewMember);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        // Parse the new member. This will also validate the new member against the case team
        newCaseTeamMember = new Member(caseInstance.getCaseTeam(), jsonNewMember, caseInstance);
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        Team caseTeam = caseInstance.getCaseTeam();
        caseTeam.addMember(newCaseTeamMember);
        return new CaseResponse(this);
    }

}
