package org.cafienne.cmmn.expression.spel.api.cmmn.team;

import org.cafienne.cmmn.expression.spel.api.APIObject;
import org.cafienne.cmmn.instance.Case;
import org.cafienne.cmmn.instance.team.Member;

/**
 */
public class MemberAPI extends APIObject<Case> {
    private final Member member;

    public MemberAPI(Member member) {
        super(member.getCaseInstance());
        this.member = member;
        addPropertyReader("isUser", this::isUser);
        addPropertyReader("isOwner", this::isOwner);
        addPropertyReader("id", member.key::id);
        addPropertyReader("type", member.key::type);
    }

    boolean isUser() {
        return member.isUser();
    }

    boolean isOwner() {
        return member.isOwner();
    }
}
