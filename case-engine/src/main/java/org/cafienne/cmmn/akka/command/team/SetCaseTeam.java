package org.cafienne.cmmn.akka.command.team;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Team;
import org.cafienne.cmmn.akka.command.CaseCommand;

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

    private final CaseTeam caseTeam;
    private Team newCaseTeam;

    private enum Fields {
        team
    }

    public SetCaseTeam(TenantUser tenantUser, String caseInstanceId, CaseTeam caseTeam) {
        // TODO: determine how to do authorization on this command.
        super(tenantUser, caseInstanceId);
        this.caseTeam = caseTeam;
    }

    public SetCaseTeam(ValueMap json) {
        super(json);
        this.caseTeam = new CaseTeam(readArray(json, Fields.team));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.team, caseTeam);
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);
        // Parse the new case team. This will also validate the team
        newCaseTeam = new Team(caseTeam, caseInstance, caseInstance.getDefinition());
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        Team caseTeam = caseInstance.getCaseTeam();
        // Clear the existing members
        new ArrayList<>(caseTeam.getMembers()).forEach(caseTeam::removeMember);
        // Add new members
        newCaseTeam.getMembers().forEach(caseTeam::addMember);
        return new CaseResponse(this);
    }

}
