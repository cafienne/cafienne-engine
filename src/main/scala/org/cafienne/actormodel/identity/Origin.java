package org.cafienne.actormodel.identity;

public enum Origin {
    Tenant("Tenant"),
    Platform("Platform"),
    IDP("IDP"),
    PlatformOwner("PlatformOwner"),
    TimerService("TimerService"),
    Anonymous("Anonymous");

    private final String value;

    Origin(String value) {
        this.value = value;
    }

    public boolean isTenant() {
        return this == Tenant;
    }

    public boolean isPlatform() {
        return this == Platform;
    }

    public boolean isIDP() {
        return this == IDP;
    }

    @Override
    public String toString() {
        return value;
    }

    public static Origin getEnum(String value) {
        if (value == null) return null;
        for (Origin origin : values())
            if (origin.toString().equalsIgnoreCase(value)) return origin;
        return null;
    }
}
