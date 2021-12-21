package org.cafienne.cmmn.actorapi.event.team.deprecated.user;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.cmmn.actorapi.event.team.deprecated.DeprecatedCaseTeamEvent;
import org.cafienne.infrastructure.serialization.Fields;
import org.cafienne.json.ValueMap;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * TeamMemberAdded and TeamMemberRemoved are no longer generated
 */
public abstract class DeprecatedCaseTeamUserEvent extends DeprecatedCaseTeamEvent {
    protected final String userId;
    protected final Set<String> roles;

    protected DeprecatedCaseTeamUserEvent(ValueMap json) {
        super(json);
        this.userId = json.readString(Fields.userId);
        this.roles = json.readSet(Fields.roles);
    }

    @Override
    public boolean isUserEvent() {
        return true;
    }

    @Override
    public String getId() {
        return userId;
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
