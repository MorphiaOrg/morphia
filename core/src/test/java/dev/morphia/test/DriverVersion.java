package dev.morphia.test;

import org.semver4j.Semver;

public enum DriverVersion {
    ANY(0, 0),
    v41(4, 1),
    v42(4, 2),
    v43(4, 3),
    v45(4, 5),
    v46(4, 6),
    v47(4, 7),
    v52(5, 2);

    private final int major;

    private final int minor;

    DriverVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public final Semver version() {
        return Semver.of(major, minor, 0);
    }
}
