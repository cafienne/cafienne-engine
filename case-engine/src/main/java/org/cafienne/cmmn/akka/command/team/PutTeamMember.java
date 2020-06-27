package org.cafienne.cmmn.akka.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.response.CaseResponse;
import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.CaseTeamError;
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
public class PutTeamMember extends CaseTeamMemberCommand {
    private final CaseTeamMember newMember;

    private enum Fields {
        member
    }

    public PutTeamMember(TenantUser tenantUser, String caseInstanceId, CaseTeamMember newMember) {
        super(tenantUser, caseInstanceId, newMember.key());
        this.newMember = newMember;
    }

    public PutTeamMember(ValueMap json) {
        super(json);
        newMember = CaseTeamMember.deserialize(readMap(json, Fields.member));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.write(generator);
        writeField(generator, Fields.member, newMember.toValue());
    }

    @Override
    public void validate(Case caseInstance) {
        super.validate(caseInstance);

        Member existingTeamMember = caseInstance.getCaseTeam().getMember(key());
        if (existingTeamMember != null) {
            // If the ownership changes, and it is removed, then check there is at least one owner left in the team.
            if (newMember.isOwner().nonEmpty() && !newMember.isOwner().getOrElse(() -> false)) {
                // check that there are sufficient owners
                super.validateWhetherOwnerCanBeRemoved(caseInstance, key());
            }
        }

        // Check whether the new roles are valid and do not conflict
        super.validateCaseTeamRoles(caseInstance, newMember);

        // Check whether roles need to be removed from the member; if so, the roles must be valid CaseRoles.
        newMember.validateRolesExist(caseInstance.getDefinition());
    }

    @Override
    public CaseResponse process(Case caseInstance) {
        Team caseTeam = caseInstance.getCaseTeam();
        caseTeam.upsert(newMember);
        return new CaseResponse(this);
    }

}
