package dev.morphia.test;

import org.semver4j.Semver;

public enum ServerVersion {
    ANY(0, 0),
    v50(5, 0),
    v51(5, 1),
    v52(5, 2),
    v53(5, 3),
    v60(6, 0),
    v63(6, 3),
    v70(7, 0),
    v80(8, 0);

    private final int major;

    private final int minor;

    ServerVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public final Semver version() {
        return Semver.of(major, minor, 0);
    }
}
