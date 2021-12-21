package org.cafienne.cmmn.instance.team;

public enum MemberType {
    User("User"),
    TenantRole("TenantRole"),
    Group("Group");

    private String value;

    MemberType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public boolean isUser() {
        return this == User;
    }

    public boolean isTenantRole() {
        return this == TenantRole;
    }

    public boolean isGroup() {
        return this == Group;
    }
}