package dev.morphia.test;

import com.github.zafarkhaja.semver.Version;

public enum ServerVersion {
    ANY(0, 0),
    v51(5, 1),
    v52(5, 2),
    v53(5, 3),
    v60(6, 0),
    v63(6, 3);

    private final int major;

    private final int minor;

    ServerVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public final Version version() {
        return Version.forIntegers(major, minor);
    }
}
