package dev.morphia.test;

import com.github.zafarkhaja.semver.Version;

public enum ServerVersion {
    ANY(0,0),
    MDB51(5, 1),
    MDB52(5, 2),
    MDB53(5, 3),
    MDB60(6, 0);

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
