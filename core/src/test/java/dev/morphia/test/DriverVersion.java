package dev.morphia.test;

import com.github.zafarkhaja.semver.Version;

public enum DriverVersion {
    ANY(0, 0),
    v41(4, 1),
    v42(4, 2),
    v45(4, 5),
    v46(4, 6),
    v47(4, 7);

    private final int major;

    private final int minor;

    DriverVersion(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public final Version version() {
        return Version.forIntegers(major, minor);
    }
}
