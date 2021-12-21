package org.cafienne.cmmn.actorapi.command.team;

import org.cafienne.json.ValueMap;

/**
 * Simple functional interface to read a typed CaseTeamMember from a ValueMap
 */
@FunctionalInterface
public interface CaseTeamMemberDeserializer<Member extends CaseTeamMember> {
    Member readMember(ValueMap json);
}
