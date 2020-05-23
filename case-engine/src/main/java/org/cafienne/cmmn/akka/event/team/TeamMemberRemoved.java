package org.cafienne.cmmn.akka.event.team;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.cafienne.akka.actor.serialization.Manifest;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.instance.team.Member;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Event caused when a member is removed from the case team.
 */
@Manifest
public class TeamMemberRemoved extends CaseTeamEvent {
    private final String userId;
    private final Set<String> roles = new HashSet();

    public TeamMemberRemoved(Case caseInstance, Member member) {
        super(caseInstance);
        this.userId = member.getUserId();
        for (CaseRoleDefinition role : member.getRoles()) {
            roles.add(role.getName());
        }
    }

    public TeamMemberRemoved(ValueMap json) {
        super(json);
        this.userId = json.raw(Fields.userId);
        json.withArray(Fields.roles).getValue().forEach(role -> roles.add((String) role.getValue()));
    }
    
    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.roles, roles);
    }

    @Override
    public void updateState(Case actor) {
        actor.getCaseTeam().updateState(this);
    }

    /**
     * Id of user that is removed.
     * @return
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Roles the member had.
     */
    public Set<String> getRoles() {
        return roles;
    }
}
