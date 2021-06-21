package org.cafienne.cmmn.actorapi.event.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.cmmn.actorapi.command.team.MemberKey;
import org.cafienne.cmmn.definition.team.CaseRoleDefinition;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.json.ValueMap;
import org.cafienne.cmmn.instance.team.Member;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TeamMemberAdded and TeamMemberRemoved are no longer generated
 */
public abstract class DeprecatedCaseTeamEvent extends CaseTeamEvent {
    protected final String userId;
    protected final Set<String> roles = new HashSet();
    public final transient MemberKey key;

    protected DeprecatedCaseTeamEvent(Case caseInstance, Member member) {
        super(caseInstance);
        this.key = member.key;
        this.userId = member.getMemberId();
        for (CaseRoleDefinition role : member.getRoles()) {
            roles.add(role.getName());
        }
    }

    protected DeprecatedCaseTeamEvent(ValueMap json) {
        super(json);
        this.userId = json.raw(Fields.userId);
        json.withArray(Fields.roles).getValue().forEach(role -> roles.add((String) role.getValue()));
        this.key = new MemberKey(userId, "user");
    }

    /**
     * Name/id of user that is added or removed. Isolating logic in a single place
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

    @Override
    public String getDescription() {
        if (roles.size() == 1) {
            return getClass().getSimpleName() + "['" + userId + "' left team]";
        } else {
            String rolesString = (roles.stream().filter(role -> !role.isBlank()).map(role -> "'" + role + "'").collect(Collectors.joining(", ")));
            return getClass().getSimpleName() + "['" +userId + "' with roles " + rolesString + " left the team]";
        }
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        super.writeCaseTeamEvent(generator);
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.roles, roles);
    }
}
