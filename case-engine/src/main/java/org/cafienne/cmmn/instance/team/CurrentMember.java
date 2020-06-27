package org.cafienne.cmmn.instance.team;

import org.cafienne.akka.actor.identity.TenantUser;
import org.cafienne.cmmn.akka.command.team.MemberKey;
import org.cafienne.cmmn.definition.CaseDefinition;
import org.cafienne.cmmn.definition.CaseRoleDefinition;
import org.w3c.dom.Element;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A temporary member in the case team, holding all bound roles that are currently applicable for the user.
 * If a user that has a proper tenant role performs an action on the case, that user will not be persisted in the
 * akka event store, but still it is handy to work with such a user
 */
public class CurrentMember extends Member {
    private final TenantUser user;

    /**
     * Create a temporary member object for tenant user
     *
     * @param team
     * @param user
     */
    CurrentMember(Team team, TenantUser user) {
        super(team, new MemberKey(user.id(), "user"));
        this.user = user;
    }

    @Override
    public boolean isOwner() {
        return getMatchingMembers().filter(member -> member.isOwner()).count() > 0;
    }

    /**
     * Returns the roles currently assigned to this member
     *
     * @return
     */
    public Set<CaseRoleDefinition> getRoles() {
        Set<CaseRoleDefinition> roles = new HashSet<>();
        getMatchingMembers().forEach(member -> roles.addAll(member.getRoles()));
        return roles;
    }

    private Stream<Member> getMatchingMembers() {
        return getTeam().getMembers().stream().filter(member -> member.key.equals(this.key) || (!member.isUser() && user.roles().contains(member.key.id())));
    }

    public void dumpMemoryStateToXML(Element parentElement) {
        Element memberXML = parentElement.getOwnerDocument().createElement("Member");
        parentElement.appendChild(memberXML);
        memberXML.setAttribute("name", getMemberId());
        memberXML.setAttribute("roles", getRoles().toString());
        // roles.forEach(role -> {
        // Element roleXML = parentElement.getOwnerDocument().createElement("Role");
        // memberXML.appendChild(roleXML);
        // roleXML.appendChild(parentElement.getOwnerDocument().createTextNode(role.getName()));
        // });
    }

    @Override
    public CaseDefinition getDefinition() {
        return getCaseInstance().getDefinition();
    }
}
