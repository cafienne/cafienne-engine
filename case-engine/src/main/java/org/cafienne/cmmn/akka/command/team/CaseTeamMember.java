package org.cafienne.cmmn.akka.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.AkkaSerializable;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CaseTeamMember implements AkkaSerializable {

    private String userId;
    private Set<String> roles = new HashSet();

    private enum Fields {
        userId, roles
    }

    @Deprecated
    public CaseTeamMember() {
    }

    public CaseTeamMember(String userId, String ...roles) {
        this.userId = userId;
        this.roles = Arrays.asList(roles).stream().collect(Collectors.toSet());
    }

    public CaseTeamMember(String userId, Set<String> roles) {
        this.userId = userId;
        this.roles = roles;
    }

    public CaseTeamMember(ValueMap json) {
        this.userId = readField(json, Fields.userId);
        readArray(json, Fields.roles).forEach(role -> roles.add(role.getValue().toString()));
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeStartObject();
        writeField(generator, Fields.userId, userId);
        writeField(generator, Fields.roles, roles);
        generator.writeEndObject();
    }

    public String getUser() {
        return userId;
    }

    public void setUser(String userId) {
        this.userId = userId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseTeamMember that = (CaseTeamMember) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(roles, that.roles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roles);
    }
}
