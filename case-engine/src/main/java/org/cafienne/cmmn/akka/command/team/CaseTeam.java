package org.cafienne.cmmn.akka.command.team;

import com.fasterxml.jackson.core.JsonGenerator;
import org.cafienne.akka.actor.serialization.AkkaSerializable;
import org.cafienne.cmmn.instance.casefile.ValueList;
import org.cafienne.cmmn.instance.casefile.ValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class CaseTeam implements AkkaSerializable {
    private List<CaseTeamMember> members = new ArrayList();

    public CaseTeam() {
    }

    public CaseTeam(CaseTeamMember ...members) {
        this(Arrays.asList(members));
    }

    public CaseTeam(List<CaseTeamMember> members) {
        this.members = members;
    }

    public CaseTeam(ValueList json) {
        json.forEach(member -> members.add(new CaseTeamMember((ValueMap)member)));
    }

    public List<CaseTeamMember> getMembers() {
        return members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseTeam caseTeam = (CaseTeam) o;
        return Objects.equals(members, caseTeam.members);
    }

    @Override
    public int hashCode() {
        return Objects.hash(members);
    }

    @Override
    public void write(JsonGenerator generator) throws IOException {
        generator.writeStartArray();
        for (CaseTeamMember member : members) member.write(generator);
        generator.writeEndArray();
    }
}
