package org.cafienne.infrastructure.config.persistence.eventdb;

public enum Profile {
    SQLServer,
    Postgres,
    H2,
    Unsupported;

    private String value;

    public static Profile from(String value) {
        if (value.contains("Postgres")) {
            return Profile.Postgres.with(value);
        } else if (value.contains("SQLServer")) {
            return Profile.SQLServer.with(value);
        } else if (value.contains("H2")) {
            return Profile.H2.with(value);
        } else {
            return Profile.Unsupported.with(value);
        }
    }

    public boolean isPostgres() {
        return this == Postgres;
    }

    public boolean isSQLServer() {
        return this == SQLServer;
    }

    public boolean isH2() {
        return this == H2;
    }

    private Profile with(String value) {
        this.value = value;
        return this;
    }

    public String getValue() {
        return this.value;
    }
}
